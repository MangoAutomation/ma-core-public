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

import com.serotonin.m2m2.DataType;
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
    private IdPointValueTime lastValue;
    private volatile long timestamp;

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
        long readCount = 0L, writeCount = 0L;
        long startTime = System.currentTimeMillis();
        try {
            ReadWriteCount counts = parent.getRetry().executeCallable(this::migrateNextPeriod);
            readCount = counts.getReadCount();
            writeCount = counts.getWriteCount();
        } catch (Exception e) {
            this.status = MigrationStatus.ERROR;
            if (log.isErrorEnabled()) {
                log.error("Migration aborted for {}", this, e);
            }
        }
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTime);
        parent.updateProgress(this, readCount, writeCount, duration);
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

                // truncate the timestamp, so we get aggregates starting at a consistent round time
                // should not affect the migration of raw values
                this.timestamp = timestamp.atZone(parent.getClock().getZone())
                        .truncatedTo(parent.getTruncateTo())
                        .toInstant()
                        .toEpochMilli();

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

    private ReadWriteCount migrateNextPeriod() {
        if (status.isComplete()) throw new IllegalStateException("Already complete");

        if (point == null) {
            this.point = Objects.requireNonNull(parent.getDataPointDao().getBySeriesId(seriesId));
        }
        if (status == MigrationStatus.NOT_STARTED) {
            initialPass();
            return new ReadWriteCount();
        }

        this.status = MigrationStatus.RUNNING;

        if (!lastValueInitialized) {
            this.lastValue = parent.getSource().getPointValueBefore(point, timestamp)
                    .map(v -> v.withSeriesId(point.getSeriesId()))
                    .orElse(null);
            this.lastValueInitialized = true;
        }

        Clock clock = parent.getClock();
        ZonedDateTime from = Instant.ofEpochMilli(this.timestamp).atZone(clock.getZone());
        ZonedDateTime to = from.plus(parent.getPeriod());

        if (clock.instant().isBefore(to.toInstant())) {
            // close out series, do the final copy while holding the write-lock so inserts are blocked
            lock.writeLock().lock();
            try {
                // copy an extra period in case the current time rolls over into the next period
                ReadWriteCount sampleCount = copyPointValues(from, to.plus(parent.getPeriod()));
                this.status = MigrationStatus.MIGRATED;
                if (log.isDebugEnabled()) {
                    log.debug("Migration finished for {}", this);
                }
                return sampleCount;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            return copyPointValues(from, to);
        }
    }

    /**
     * stream from source (old) db to the destination (new) db, aggregating if configured
     */
    private ReadWriteCount copyPointValues(ZonedDateTime from, ZonedDateTime to) {
        TemporalAmount aggregationPeriod = parent.getAggregationPeriod();
        TemporalAmount aggregationDelay = parent.getAggregationDelay();
        ReadWriteCount sampleCount = new ReadWriteCount();

        Clock clock = parent.getClock();
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime transition = now.minus(aggregationDelay);
        long transitionMs = transition.toInstant().toEpochMilli();

        long fromMs = from.toInstant().toEpochMilli();
        long toMs = to.toInstant().toEpochMilli();

        try (var stream = parent.getSource().streamPointValues(point, fromMs, toMs, null, TimeOrder.ASCENDING, parent.getReadChunkSize())
                .peek(pv -> sampleCount.incrementRead())
                .peek(pv -> this.lastValue = pv)) {

            if (aggregationPeriod != null && point.getPointLocator().getDataType() == DataType.NUMERIC) {
                AggregateDao source = parent.getSource().getAggregateDao(aggregationPeriod);
                AggregateDao destination = parent.getDestination().getAggregateDao(aggregationPeriod);

                // TODO filter out empty entries?
                var aggregateStream = source
                        .aggregate(point, from, to, Stream.concat(Stream.ofNullable(lastValue), stream))
                        .peek(pv -> sampleCount.incrementWrite());
                destination.save(point, aggregateStream, parent.getWriteChunkSize());
            } else {
                var output = stream
                        .map(pv -> new BatchPointValueImpl<>(point, pv))
                        .peek(pv -> sampleCount.incrementWrite());
                parent.getDestination().savePointValues(output, parent.getWriteChunkSize());
            }
        }
        this.timestamp = toMs;
        return sampleCount;
    }

    Long getTimestamp() {
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

    public static class ReadWriteCount {
        private long readCount;
        private long writeCount;

        private ReadWriteCount() {

        }

        public long getReadCount() {
            return readCount;
        }

        public long getWriteCount() {
            return writeCount;
        }

        private void incrementRead() {
            readCount++;
        }

        private void incrementWrite() {
            writeCount++;
        }

    }
}
