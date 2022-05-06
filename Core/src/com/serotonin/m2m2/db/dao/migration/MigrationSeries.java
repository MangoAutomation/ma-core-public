/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgress;
import com.serotonin.m2m2.db.dao.pointvalue.AggregateDao;
import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
class MigrationSeries {

    private final Logger log = LoggerFactory.getLogger(MigrationSeries.class);

    private final MigrationPointValueDao parent;
    private final int seriesId;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private MigrationStatus status;
    private DataPointVO point;
    private boolean lastValueInitialized;
    /**
     * Holds the last raw value read from this series, this is required for calculating statistics for the next
     * period (startValue of next period).
     */
    private IdPointValueTime lastValue;
    private volatile long timestamp;
    private ReadWriteStats stats = null;

    MigrationSeries(MigrationPointValueDao parent, int seriesId) {
        // use MIN_VALUE so series that haven't completed their initial pass are at the start of the priority queue
        this(parent, seriesId, MigrationStatus.NOT_STARTED, Long.MIN_VALUE);
    }

    MigrationSeries(MigrationPointValueDao parent, int seriesId, MigrationStatus status, long timestamp) {
        this.parent = parent;
        this.seriesId = seriesId;
        this.status = status;
        this.timestamp = timestamp;
    }

    synchronized void run() {
        long startTime = System.currentTimeMillis();
        try {
            this.stats = new ReadWriteStats();
            parent.getRetry().executeRunnable(this::migrateNextPeriod);
        } catch (Exception e) {
            this.status = MigrationStatus.ERROR;
            if (log.isErrorEnabled()) {
                log.error("Migration aborted for {}", this, e);
            }
        }
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTime);
        parent.updateProgress(this, duration);
    }

    private void initialPass() {
        if (!parent.getDataPointFilter().test(point)) {
            this.status = MigrationStatus.SKIPPED;
            if (log.isDebugEnabled()) {
                log.debug("Skipped {}", this);
            }
            return;
        }

        // only get the initial timestamp if migration was not started yet, otherwise we already have retrieved it from the database
        lock.writeLock().lock();
        try {
            Optional<Long> inception = parent.getSource().getInceptionDate(point);
            if (inception.isPresent()) {
                Instant timestamp = Instant.ofEpochMilli(inception.get());
                Instant migrateFrom = parent.getMigrateFrom();
                if (migrateFrom != null && migrateFrom.isAfter(timestamp)) {
                    timestamp = migrateFrom;
                }

                if (aggregationEnabledForPoint()) {
                    // truncate the timestamp, so we get aggregates starting at a consistent round time
                    var dateTime = timestamp.atZone(parent.getClock().getZone());
                    this.timestamp = truncateToAggregationPeriod(dateTime)
                            .toInstant()
                            .toEpochMilli();
                } else {
                    this.timestamp = timestamp.toEpochMilli();
                }

                this.status = MigrationStatus.INITIAL_PASS_COMPLETE;
                if (log.isDebugEnabled()) {
                    log.debug("Initial pass complete {}", this);
                }
            } else {
                // no data
                this.status = MigrationStatus.NO_DATA;
                if (log.isDebugEnabled()) {
                    log.debug("Series contained no data {}", this);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private ZonedDateTime truncateToAggregationPeriod(ZonedDateTime dateTime) {
        var aggregateDao = parent.getDestination().getAggregateDao();
        return aggregateDao.truncateToPeriod(dateTime, parent.getAggregationPeriod());
    }

    private void migrateNextPeriod() {
        if (status.isComplete()) throw new IllegalStateException("Already complete");

        Clock clock = parent.getClock();
        ZonedDateTime initialPosition = Instant.ofEpochMilli(timestamp)
                .atZone(clock.getZone());

        if (point == null) {
            this.point = Objects.requireNonNull(parent.getDataPointDao().getBySeriesId(seriesId));
        }
        if (status == MigrationStatus.NOT_STARTED) {
            initialPass();
        }

        this.status = MigrationStatus.RUNNING;

        if (!lastValueInitialized) {
            this.lastValue = parent.getSource().getPointValueBefore(point, timestamp)
                    .map(v -> v.withSeriesId(point.getSeriesId()))
                    .orElse(null);
            this.lastValueInitialized = true;
        }

        Duration period = parent.getPeriod();
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime start = Instant.ofEpochMilli(timestamp).atZone(clock.getZone());
        ZonedDateTime rawEnd = start.plus(period);
        boolean copyRawValues = true;

        if (aggregationEnabledForPoint()) {
            ZonedDateTime boundary = truncateToAggregationPeriod(now.minus(parent.getAggregationBoundary()));
            ZonedDateTime rawBoundary = boundary.minus(parent.getAggregationOverlap());
            copyRawValues = start.isAfter(rawBoundary) || start.isEqual(rawBoundary);

            if (start.isBefore(boundary)) {
                ZonedDateTime end = minimum(
                        start.plus(period.multipliedBy(parent.getPeriodMultiplier())),
                        boundary
                );

                if (copyRawValues) {
                    // if we have started copying raw values, don't allow copying aggregates past the raw end time
                    copyAggregateValues(start, minimum(end, rawEnd));
                } else {
                    copyAggregateValues(start, end);
                    this.timestamp = rawEnd.toInstant().toEpochMilli();
                }
            }
        }

        if (copyRawValues) {
            if (rawEnd.isAfter(now)) {
                // close out series, do the final copy while holding the write-lock so inserts are blocked
                lock.writeLock().lock();
                try {
                    // copy an extra period in case the current time ticks over into the next period
                    copyRawValues(start, rawEnd.plus(parent.getPeriod()));
                    this.status = MigrationStatus.MIGRATED;
                    if (log.isDebugEnabled()) {
                        log.debug("Migration finished for {}", this);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                copyRawValues(start, rawEnd);
            }
            this.timestamp = rawEnd.toInstant().toEpochMilli();
        }

        ZonedDateTime finalPosition = Instant.ofEpochMilli(timestamp)
                .atZone(clock.getZone());
        stats.setMigratedDuration(Duration.between(initialPosition, finalPosition));
    }

    private ZonedDateTime minimum(ZonedDateTime a, ZonedDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private boolean aggregationEnabledForPoint() {
        TemporalAmount aggregationPeriod = parent.getAggregationPeriod();
        return aggregationPeriod != null &&
                parent.getAggregationDataTypes().contains(point.getPointLocator().getDataType());
    }

    /**
     * Copy raw values from source (old) db to the destination (new) db.
     */
    private void copyRawValues(ZonedDateTime from, ZonedDateTime to) {
        long fromMs = from.toInstant().toEpochMilli();
        long toMs = to.toInstant().toEpochMilli();

        try (var stream = parent.getSource()
                .streamPointValues(point, fromMs, toMs, null, TimeOrder.ASCENDING, parent.getReadChunkSize())
                .peek(pv -> stats.incrementRead())) {
            var output = stream
                    .map(pv -> new BatchPointValueImpl<>(point, pv))
                    .peek(pv -> stats.incrementWrite());
            parent.getDestination().savePointValues(output, parent.getWriteChunkSize());
        }
    }

    /**
     * Copy aggregate values from source (old) db to the destination (new) db.
     */
    private void copyAggregateValues(ZonedDateTime from, ZonedDateTime to) {
        long fromMs = from.toInstant().toEpochMilli();
        long toMs = to.toInstant().toEpochMilli();

        // We could use the source AggregateDao's query method here in order to migrate pre-aggregated data from one
        // DB to another. There are 2 caveats: we must provide a way to get the read speed from the query method,
        // and we must provide a way to pass in the last value. If we do not retain the last value and instead
        // let the query method call getPointValueBefore() we get far lower total read speeds.

        try (var stream = parent.getSource()
                .streamPointValues(point, fromMs, toMs, null, TimeOrder.ASCENDING, parent.getReadChunkSize())
                .peek(pv -> stats.incrementRead())
                .peek(pv -> this.lastValue = pv)) {

            AggregateDao source = parent.getSource().getAggregateDao();
            AggregateDao destination = parent.getDestination().getAggregateDao();

            var aggregateStream = source
                    .aggregate(point, from, to, Stream.concat(Stream.ofNullable(lastValue), stream), parent.getAggregationPeriod())
                    .peek(pv -> stats.incrementAggregateWrite());
            destination.save(point, aggregateStream, parent.getWriteChunkSize());
        }
    }

    long getTimestamp() {
        return timestamp;
    }

    boolean isMigrated() {
        lock.readLock().lock();
        try {
            return status == MigrationStatus.MIGRATED;
        } finally {
            lock.readLock().unlock();
        }
    }

    int getSeriesId() {
        return seriesId;
    }

    MigrationStatus getStatus() {
        return status;
    }

    ReadWriteStats getStats() {
        return stats;
    }

    MigrationProgress getMigrationProgress() {
        return new MigrationProgress(seriesId, status, timestamp);
    }

    @Override
    public String toString() {
        return "MigrationSeries{" +
                "seriesId=" + seriesId +
                ", point=" + point +
                '}';
    }

    public static class ReadWriteStats {

        private long readCount;
        private long writeCount;
        private long aggregateWriteCount;
        private Duration migratedDuration = Duration.ZERO;

        private ReadWriteStats() {
        }

        public long getReadCount() {
            return readCount;
        }

        public long getWriteCount() {
            return writeCount;
        }

        public long getAggregateWriteCount() {
            return aggregateWriteCount;
        }

        private void incrementRead() {
            readCount++;
        }

        private void incrementWrite() {
            writeCount++;
        }

        private void incrementAggregateWrite() {
            aggregateWriteCount++;
        }

        public Duration getMigratedDuration() {
            return migratedDuration;
        }

        public void setMigratedDuration(Duration migratedDuration) {
            this.migratedDuration = migratedDuration;
        }
    }
}
