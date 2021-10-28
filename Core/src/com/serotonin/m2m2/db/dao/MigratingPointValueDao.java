/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.mutable.MutableLong;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.vo.DataPointVO;

public class MigratingPointValueDao extends DelegatingPointValueDao implements AutoCloseable {

    /**
     * Separate log file is configured for this logger
     */
    private final Logger log = LoggerFactory.getLogger("pointValueMigration");
    /**
     * Map key is series id
     */
    private final ConcurrentMap<Integer, MigrationStatus> migratedSeries = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Integer> seriesQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean fullyMigrated = new AtomicBoolean();
    private final AtomicBoolean stopFlag = new AtomicBoolean();
    private final AtomicLong migratedCount = new AtomicLong();
    private final AtomicLong skippedCount = new AtomicLong();
    private final AtomicLong migratedTotal = new AtomicLong();
    private final MigrationThread[] threads;
    private final Predicate<DataPointVO> migrationFilter;
    private final DataPointDao dataPointDao;

    public enum MigrationStatus {
        NOT_STARTED,
        RUNNING,
        MIGRATED,
        SKIPPED
    }

    // TODO add time filter
    public MigratingPointValueDao(PointValueDao primary, PointValueDao secondary, DataPointDao dataPointDao, Predicate<DataPointVO> migrationFilter) {
        super(primary, secondary);
        // TODO configurable
        this.threads = new MigrationThread[4];
        this.dataPointDao = dataPointDao;
        this.migrationFilter = migrationFilter;
    }

    @PostConstruct
    private void postConstruct() {
        try (var stream = dataPointDao.streamSeriesIds()) {
            stream.forEach(seriesId -> {
                migratedSeries.put(seriesId, MigrationStatus.NOT_STARTED);
                seriesQueue.add(seriesId);
                migratedTotal.incrementAndGet();
            });
        }

        for (int i = 0; i < threads.length; i++) {
            MigrationThread thread = new MigrationThread(i);
            this.threads[i] = thread;
            thread.start();
        }
    }

    @Override
    public void close() throws Exception {
        stopFlag.set(true);
        for (MigrationThread thread : threads) {
            if (thread != null) {
                thread.join(TimeUnit.SECONDS.toMillis(60));
                thread.interrupt();
            }
        }
    }

    @Override
    public boolean handleWithPrimary(Operation operation) {
        if (fullyMigrated.get()) {
            return true;
        }
        throw new UnsupportedOperationException();
    }

    public boolean handleWithPrimary(DataPointVO vo, Operation operation) {
        @Nullable MigrationStatus migrated = migratedSeries.get(vo.getSeriesId());
        if (migrated == null || migrated == MigrationStatus.MIGRATED) {
            // series is a new series, or has been migrated
            return true;
        }

        if (operation == Operation.DELETE) {
            throw new UnsupportedOperationException();
        }
        return false;
    }

    private class MigrationThread extends Thread {

        private MigrationThread(int threadId) {
            super();
            setName(String.format("pv-migration-%03d", threadId + 1));
        }

        @Override
        public void run() {
            if (log.isInfoEnabled()) {
                log.info("{} Migration thread starting", stats());
            }

            Integer seriesId = null;
            while (!stopFlag.get() && (seriesId = seriesQueue.poll()) != null) {
                MigrationStatus previous = migratedSeries.put(seriesId, MigrationStatus.RUNNING);
                if (previous != MigrationStatus.NOT_STARTED) {
                    throw new IllegalStateException("Migration should not have stared for series: " + seriesId);
                }

                try {
                    migrateSeries(seriesId);
                } catch (Exception e) {
                    log.error("Error migrating series {}", seriesId, e);
                    migratedSeries.put(seriesId, MigrationStatus.NOT_STARTED);
                    seriesQueue.add(seriesId);
                }
            }

            if (seriesId == null) {
                if (log.isInfoEnabled()) {
                    log.info("{} Migration complete, no more work", stats());
                }
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("{} Migration interrupted", stats());
                }
            }
        }

        private void migrateSeries(Integer seriesId) {
            DataPointVO point = dataPointDao.getBySeriesId(seriesId);
            if (point == null || !migrationFilter.test(point)) {
                migratedSeries.put(seriesId, MigrationStatus.SKIPPED);
                skippedCount.incrementAndGet();
                if (log.isInfoEnabled()) {
                    log.info("{} Skipped point {} (seriesId={})", stats(), point == null ? null : point.getXid(), seriesId);
                }
                return;
            }

            MutableLong sampleCount = new MutableLong();
            // initial pass at copying data
            Long lastTimestamp = copyPointValues(point, null, sampleCount);
            // copy again as the first run might have taken a long time
            lastTimestamp = copyPointValues(point, lastTimestamp, sampleCount);

            Long lastTimestampFinal = lastTimestamp;

            // do a final copy inside the compute method so inserts are blocked
            migratedSeries.computeIfPresent(point.getSeriesId(), (k,v) -> {
                copyPointValues(point, lastTimestampFinal, sampleCount);
                return MigrationStatus.MIGRATED;
            });
            migratedCount.incrementAndGet();

            if (log.isInfoEnabled()) {
                log.info("{} Migrated point {} (seriesId={}, values={})", stats(), point.getXid(), seriesId, sampleCount.longValue());
            }
        }

        private String stats() {
            long migrated = migratedCount.get();
            long skipped = skippedCount.get();
            long total = migratedTotal.get();
            double progress = migrated * 100d / total;
            return String.format("[%6.2f%% complete, %d migrated, %d skipped, %d total]", progress, migrated, skipped, total);
        }

        private Long copyPointValues(DataPointVO point, Long from, MutableLong sampleCount) {
            // TODO configurable
            int batchSize = 10_000;
            MutableLong lastTimestamp = new MutableLong();
            MutableLong count = new MutableLong();

            try (var stream = secondary.streamPointValues(point, from, null, null, TimeOrder.ASCENDING, batchSize)) {
                // TODO save methods should support Annotated PVT if source is null?
                // TODO check flag and abort
                primary.savePointValues(stream.map(pv -> {
                    lastTimestamp.setValue(pv.getTime());
                    count.incrementAndGet();
                    return new BatchPointValueImpl(point, pv, null);
                }), batchSize);
            }

            sampleCount.add(count.longValue());
            return lastTimestamp.longValue();
        }
    }
}
