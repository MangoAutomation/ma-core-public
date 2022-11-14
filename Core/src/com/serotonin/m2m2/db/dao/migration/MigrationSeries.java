/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
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
     * block (startValue of next block).
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
        try {
            this.stats = new ReadWriteStats();
            parent.getRetry().executeCallable(Executors.callable(this::migrateBlock));
        } catch (Exception e) {
            this.status = MigrationStatus.ERROR;
            if (log.isErrorEnabled()) {
                log.error("Migration aborted: {}", this, e);
            }
        }
        parent.updateProgress(this);
    }

    private void initialPass() {
        if (!parent.getDataPointFilter().test(point)) {
            this.status = MigrationStatus.SKIPPED;
            if (log.isDebugEnabled()) {
                log.debug("Skipped: {}", this);
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
                    this.timestamp = parent.truncateToAggregationPeriod(dateTime)
                            .toInstant()
                            .toEpochMilli();
                } else {
                    this.timestamp = timestamp.toEpochMilli();
                }

                this.status = MigrationStatus.INITIAL_PASS_COMPLETE;
                if (log.isDebugEnabled()) {
                    log.debug("Initial pass complete: {}", this);
                }
            } else {
                // no data
                this.status = MigrationStatus.NO_DATA;
                if (log.isDebugEnabled()) {
                    log.debug("Series contained no data: {}", this);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void migrateBlock() {
        if (status.isComplete()) throw new IllegalStateException("Already complete");

        if (point == null) {
            this.point = Objects.requireNonNull(parent.getDataPointDao().getBySeriesId(seriesId));
        }
        if (status == MigrationStatus.NOT_STARTED) {
            initialPass();
            return;
        }

        this.status = MigrationStatus.RUNNING;

        if (!lastValueInitialized) {
            this.lastValue = parent.getSource().getPointValueBefore(point, timestamp)
                    .map(v -> v.withSeriesId(point.getSeriesId()))
                    .orElse(null);
            this.lastValueInitialized = true;
        }

        Clock clock = parent.getClock();
        Duration blockSize = parent.getBlockSize();
        Duration aggregationBlockSize = parent.getAggregationBlockSize();
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime start = Instant.ofEpochMilli(timestamp).atZone(clock.getZone());
        ZonedDateTime end = start.plus(blockSize);

        //       aggregates                       both (raw + agg)                       raw
        //     agg-block size                      std-block size                   std-block size
        // -------------------------------|------------------------------|------------------------------|
        //                         overlap-boundary                   boundary                         now

        var copyModes = EnumSet.of(CopyMode.RAW);
        if (aggregationEnabledForPoint()) {
            ZonedDateTime boundary = parent.getBoundary(start);
            ZonedDateTime overlapBoundary = boundary.minus(parent.getAggregationOverlap());

            if (start.isBefore(overlapBoundary)) {
                copyModes = EnumSet.of(CopyMode.AGGREGATES);
                end = minimum(start.plus(aggregationBlockSize), overlapBoundary);
            } else if (start.isBefore(boundary)) {
                copyModes = EnumSet.of(CopyMode.RAW, CopyMode.AGGREGATES);
            }
        }

        if (copyModes.contains(CopyMode.AGGREGATES)) {
            copyAggregateValues(start, end);
        }

        if (copyModes.contains(CopyMode.RAW)) {
            if (end.isAfter(now)) {
                // close out series, do the final copy while holding the write-lock so inserts are blocked
                lock.writeLock().lock();
                try {
                    // copy an extra block in case the current time ticks over into the next period
                    copyRawValues(start, end.plus(blockSize));
                    this.status = MigrationStatus.MIGRATED;
                    if (log.isDebugEnabled()) {
                        log.debug("Migration finished: {}", this);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                copyRawValues(start, end);
            }
        }
        this.timestamp = end.toInstant().toEpochMilli();
        stats.finished(Duration.between(start, end));
    }

    public enum CopyMode {
        RAW,
        AGGREGATES
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
        if (log.isTraceEnabled()) {
            log.trace("Copying raw values for {} from {} to {}", this, from, to);
        }

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
        if (log.isTraceEnabled()) {
            log.trace("Copying aggregate values for {} from {} to {}", this, from, to);
        }

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
                ", point=" + (point == null ? "null" : point.getXid()) +
                ", status=" + status +
                ", position=" + Instant.ofEpochMilli(timestamp) +
                '}';
    }

    public static class ReadWriteStats {

        private final Instant startTime;
        private long readCount;
        private long writeCount;
        private long aggregateWriteCount;
        private Duration migratedDuration = Duration.ZERO;

        private ReadWriteStats() {
            this.startTime = Instant.now();
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

        public void finished(Duration migratedDuration) {
            this.migratedDuration = migratedDuration;
        }

        public Instant getStartTime() {
            return startTime;
        }
    }
}
