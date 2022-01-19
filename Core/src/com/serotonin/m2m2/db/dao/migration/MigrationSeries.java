/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.PointValueDao.TimeOrder;
import com.serotonin.m2m2.db.dao.migration.MigrationProgressDao.MigrationProgress;
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
    private volatile long timestamp;

    MigrationSeries(MigrationPointValueDao parent, int seriesId) {
        this(parent, seriesId, MigrationStatus.NOT_STARTED, Long.MAX_VALUE);
    }

    MigrationSeries(MigrationPointValueDao parent, int seriesId, MigrationStatus status, long timestamp) {
        this.parent = parent;
        this.seriesId = seriesId;
        this.status = status;
        this.timestamp = timestamp;
    }

    synchronized void run() {
        long sampleCount = 0;
        long startTime = System.currentTimeMillis();
        try {
            sampleCount = parent.getRetry().executeCallable(this::migrateNextPeriod);
        } catch (Exception e) {
            this.status = MigrationStatus.ERROR;
            if (log.isErrorEnabled()) {
                log.error("Migration aborted for {}", this, e);
            }
        }
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTime);
        parent.updateProgress(this, sampleCount, duration);
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
                long timestamp = inception.get();
                Long migrateFrom = parent.getMigrateFrom();
                if (migrateFrom != null) {
                    timestamp = Math.max(timestamp, migrateFrom);
                }
                this.timestamp = timestamp;
                this.status = MigrationStatus.INITIAL_PASS_COMPLETE;
                if (log.isDebugEnabled()) {
                    log.debug("Series contained no data {}", this);
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

    private long migrateNextPeriod() {
        if (point == null) {
            this.point = Objects.requireNonNull(parent.getDataPointDao().getBySeriesId(seriesId));
        }
        if (status == MigrationStatus.NOT_STARTED) {
            initialPass();
            return 0;
        }

        long timestamp = this.timestamp;
        long from = timestamp - timestamp % parent.getPeriod();
        long to = from + parent.getPeriod();
        if (parent.getMigrateFrom() != null) {
            from = Math.max(from, parent.getMigrateFrom());
        }

        long currentTime = parent.getTimer().currentTimeMillis();
        if (currentTime < to) {
            // close out series, do the final copy while holding the write-lock so inserts are blocked
            lock.writeLock().lock();
            try {
                // copy an extra period in case the current time rolls over into the next period
                long sampleCount = copyPointValues(from, to + parent.getPeriod());
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

    private long copyPointValues(long from, long to) {
        LongAdder sampleCount = new LongAdder();
        // stream from source (old) db to the destination (new) db in chunks of matching size
        try (var stream = parent.getSource().streamPointValues(point, from, to, null, TimeOrder.ASCENDING, parent.getReadChunkSize())) {
            parent.getDestination().savePointValues(stream.map(pv -> {
                sampleCount.increment();
                return new BatchPointValueImpl(point, pv);
            }), parent.getWriteChunkSize());
        }
        this.timestamp = to;
        return sampleCount.longValue();
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
}
