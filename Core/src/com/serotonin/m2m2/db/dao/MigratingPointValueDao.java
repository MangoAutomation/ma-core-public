/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.mutable.MutableLong;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.util.properties.MangoConfigurationWatcher.MangoConfigurationReloadedEvent;

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
    private final AtomicLong migratedCount = new AtomicLong();
    private final AtomicLong skippedCount = new AtomicLong();
    private final AtomicLong migratedTotal = new AtomicLong();
    private final ArrayList<MigrationTask> tasks = new ArrayList<>();
    private final Predicate<DataPointVO> migrationFilter;
    private final DataPointDao dataPointDao;
    private final Environment env;
    private final ExecutorService executorService;

    private volatile boolean fullyMigrated = false;

    public enum MigrationStatus {
        NOT_STARTED,
        RUNNING,
        MIGRATED,
        SKIPPED
    }

    // TODO add time filter
    public MigratingPointValueDao(PointValueDao primary,
                                  PointValueDao secondary,
                                  DataPointDao dataPointDao,
                                  Predicate<DataPointVO> migrationFilter,
                                  Environment env,
                                  ExecutorService executorService,
                                  ConfigurableApplicationContext context) {
        super(primary, secondary);
        this.dataPointDao = dataPointDao;
        this.migrationFilter = migrationFilter;
        this.env = env;
        this.executorService = executorService;

        context.addApplicationListener((ApplicationListener<MangoConfigurationReloadedEvent>) this::propertiesReloaded);
    }

    // event listener annotation doesn't work here, bean registered after listeners scanned?
    private void propertiesReloaded(@SuppressWarnings("unused") MangoConfigurationReloadedEvent event) {
        adjustThreads(env.getRequiredProperty("db.migration.threadCount", int.class));
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
        adjustThreads(env.getRequiredProperty("db.migration.threadCount", int.class));
    }

    private synchronized List<MigrationTask> adjustThreads(int threadCount) {
        if (fullyMigrated || threadCount == tasks.size()) return Collections.emptyList();

        log.info("Adjusting migration threads from {} to {}", tasks.size(), threadCount);
        List<MigrationTask> stoppedTasks = new ArrayList<>();

        while (tasks.size() < threadCount) {
            MigrationTask task = new MigrationTask(tasks.size() + 1);
            tasks.add(task);
            task.getFinished().whenComplete((v, e) -> {
                if (e == null) {
                    this.fullyMigrated = true;
                }
                removeTask(task);
            });
            executorService.execute(task);
        }

        while (tasks.size() > threadCount) {
            MigrationTask task = tasks.remove(tasks.size() - 1);
            task.stopTask();
            stoppedTasks.add(task);
        }

        return stoppedTasks;
    }

    private synchronized void removeTask(MigrationTask task) {
        tasks.remove(task);
    }

    @Override
    public void close() throws Exception {
        for (MigrationTask task : adjustThreads(0)) {
            task.getFinished().get(60, TimeUnit.SECONDS);
            if (!task.getFinished().isDone()) {
                log.error("Failed to stop migration task {}", task.getName());
            }
        }
    }

    @Override
    public boolean handleWithPrimary(Operation operation) {
        if (fullyMigrated) return true;
        throw new UnsupportedOperationException();
    }

    public boolean handleWithPrimary(DataPointVO vo, Operation operation) {
        if (fullyMigrated) return true;

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

    private class MigrationTask implements Runnable {

        private final CompletableFuture<Void> finished = new CompletableFuture<>();
        private final String name;

        private MigrationTask(int threadId) {
            super();
            this.name = String.format("pv-migration-%03d", threadId);
        }

        @Override
        public void run() {
            try {
                Thread.currentThread().setName(name);
                if (log.isInfoEnabled()) {
                    log.info("{} Migration task starting", stats());
                }

                Integer seriesId;
                while (!finished.isDone() && (seriesId = seriesQueue.poll()) != null) {
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

                if (!finished.isDone()) {
                    if (log.isInfoEnabled()) {
                        log.info("{} Migration complete, no more work", stats());
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("{} Migration task cancelled", stats());
                    }
                }
            } catch (Exception e) {
                finished.completeExceptionally(e);
            } finally {
                finished.complete(null);
            }
        }

        private void stopTask() {
            finished.cancel(false);
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
            int batchSize = env.getRequiredProperty("db.migration.batchSize", int.class);
            MutableLong lastTimestamp = new MutableLong();
            MutableLong count = new MutableLong();

            try (var stream = secondary.streamPointValues(point, from, null, null, TimeOrder.ASCENDING, batchSize)) {
                primary.savePointValues(stream.map(pv -> {
                    lastTimestamp.setValue(pv.getTime());
                    count.incrementAndGet();
                    return new BatchPointValueImpl(point, pv);
                }), batchSize);
            }

            sampleCount.add(count.longValue());
            return lastTimestamp.longValue();
        }

        public CompletableFuture<Void> getFinished() {
            return finished;
        }

        public String getName() {
            return name;
        }
    }
}
