/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.PointValueDao.TimeOrder;
import com.serotonin.m2m2.db.dao.migration.MigrationProgressDao.MigrationProgress;
import com.serotonin.m2m2.vo.DataPointVO;

import io.github.resilience4j.retry.Retry;

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
    private volatile long timestamp;
    private long sampleCount;
    private boolean initialPassComplete;

    MigrationSeries(MigrationPointValueDao parent, int seriesId) {
        this(parent, seriesId, MigrationStatus.NOT_STARTED, 0L);
    }

    MigrationSeries(MigrationPointValueDao parent, int seriesId, MigrationStatus status, long timestamp) {
        this.parent = parent;
        this.seriesId = seriesId;
        this.status = status;
        this.timestamp = timestamp;
    }

    synchronized MigrationStatus run() {
        try {
            parent.retry.executeRunnable(this::migrateNextPeriod);
        } catch (Exception e) {
            parent.erroredSeries.incrementAndGet();
            this.status = MigrationStatus.ERROR;
            log.error("Error migrating period, migration aborted for point {} (seriesId={})", point != null ? point.getXid() : null, seriesId, e);
        } catch (Throwable t) {
            parent.erroredSeries.incrementAndGet();
            this.status = MigrationStatus.ERROR;
            throw t;
        }

        try {
            parent.migrationProgressDao.update(getMigrationProgress());
        } catch (Exception e) {
            log.warn("Failed to save migration progress to database for point {} (seriesId={})", point != null ? point.getXid() : null, seriesId, e);
        }

        return status;
    }

    private void initialPass() {
        this.point = parent.dataPointDao.getBySeriesId(seriesId);
        if (point == null || !parent.dataPointFilter.test(point)) {
            this.status = MigrationStatus.SKIPPED;
            parent.skippedSeries.incrementAndGet();
            if (log.isInfoEnabled()) {
                log.info("{} Skipped point {} (seriesId={})", parent.stats(), point != null ? point.getXid() : null, seriesId);
            }
        } else if (this.status == MigrationStatus.NOT_STARTED) {
            // only get the initial timestamp if migration was not started yet, otherwise we already have retrieved it from the database
            lock.writeLock().lock();
            try {
                Optional<Long> inception = parent.secondary.getInceptionDate(point);
                if (inception.isPresent()) {
                    this.status = MigrationStatus.RUNNING;
                    long timestamp = inception.get();
                    if (parent.migrateFrom != null) {
                        timestamp = Math.max(timestamp, parent.migrateFrom);
                    }
                    this.timestamp = timestamp;
                } else {
                    // no data
                    this.status = MigrationStatus.MIGRATED;
                    parent.migratedSeries.incrementAndGet();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        this.initialPassComplete = true;
    }

    private void migrateNextPeriod() {
        if (!initialPassComplete) {
            initialPass();
            return;
        }

        long startTime = System.currentTimeMillis();

        long timestamp = this.timestamp;
        long from = timestamp - timestamp % parent.period;
        long to = from + parent.period;
        if (parent.migrateFrom != null) {
            from = Math.max(from, parent.migrateFrom);
        }
        long fromFinal = from;
        parent.currentTimestamp.updateAndGet(v -> Math.max(fromFinal, v));
        this.sampleCount = 0;

        long currentTime = parent.timer.currentTimeMillis();
        if (currentTime < to) {
            // close out series, do the final copy while holding the write-lock so inserts are blocked
            lock.writeLock().lock();
            try {
                // copy an extra period in case the current time rolls over into the next period
                copyPointValues(from, to + parent.period);
                this.status = MigrationStatus.MIGRATED;
                parent.migratedSeries.incrementAndGet();
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            copyPointValues(from, to);
        }

        long duration = System.currentTimeMillis() - startTime;
        double valuesPerSecond = sampleCount / (duration / 1000d);

        parent.valuesPerPeriod.update(sampleCount);
        parent.writeMeter.mark(sampleCount);

        if (log.isDebugEnabled()) {
            log.debug("{} Completed period for {}, from {} to {} (seriesId={}, values={}, duration={}, speed={} values/s)",
                    parent.stats(), point.getXid(), Instant.ofEpochMilli(from), Instant.ofEpochMilli(to),
                    seriesId, sampleCount, Duration.ofMillis(duration),
                    String.format("%.1f", valuesPerSecond));
        }
    }

    private void copyPointValues(long from, long to) {
        this.sampleCount = 0;
        // stream from secondary (old) db to the primary db (new) in chunks of matching size
        try (var stream = parent.secondary.streamPointValues(point, from, to, null, TimeOrder.ASCENDING, parent.readChunkSize)) {
            parent.primary.savePointValues(stream.map(pv -> {
                this.sampleCount++;
                return new BatchPointValueImpl(point, pv);
            }), parent.writeChunkSize);
        }
        this.timestamp = to;
        parent.completedPeriods.incrementAndGet();
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
}
