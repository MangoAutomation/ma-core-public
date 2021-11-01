/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DelegatingPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.util.properties.MangoConfigurationWatcher.MangoConfigurationReloadedEvent;

public class MigrationPointValueDao extends DelegatingPointValueDao implements AutoCloseable {

    /**
     * Separate log file is configured for this logger
     */
    private final Logger log = LoggerFactory.getLogger("pointValueMigration");
    /**
     * Map key is series id
     */
    private final ConcurrentMap<Integer, MigrationStatus> seriesStatus = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Integer> seriesQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong migratedCount = new AtomicLong();
    private final AtomicLong totalDuration = new AtomicLong();
    private final AtomicLong skippedCount = new AtomicLong();
    private final AtomicLong totalSeries = new AtomicLong();
    private final ArrayList<MigrationTask> tasks = new ArrayList<>();
    private final Predicate<DataPointVO> migrationFilter;
    private final DataPointDao dataPointDao;
    private final Environment env;
    private final ExecutorService executorService;
    private final ConfigurableApplicationContext context;
    private final Long migrateFrom;

    private volatile boolean fullyMigrated = false;

    public enum MigrationStatus {
        NOT_STARTED,
        RUNNING,
        MIGRATED,
        SKIPPED
    }

    public MigrationPointValueDao(PointValueDao primary,
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
        this.context = context;

        String fromStr = env.getProperty("db.migration.fromDate");
        if (fromStr != null) {
            this.migrateFrom = ZonedDateTime.parse(fromStr).toInstant().toEpochMilli();
        } else {
            this.migrateFrom = null;
        }
    }

    // event listener annotation doesn't work here, bean registered after listeners scanned?
    private void propertiesReloaded(@SuppressWarnings("unused") MangoConfigurationReloadedEvent event) {
        adjustThreads(env.getRequiredProperty("db.migration.threadCount", int.class));
    }

    @PostConstruct
    private void postConstruct() {
        try (var stream = dataPointDao.streamSeriesIds()) {
            stream.forEach(seriesId -> {
                seriesStatus.put(seriesId, MigrationStatus.NOT_STARTED);
                seriesQueue.add(seriesId);
                totalSeries.incrementAndGet();
            });
        }
        context.addApplicationListener((ApplicationListener<MangoConfigurationReloadedEvent>) this::propertiesReloaded);
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
                if (e != null) {
                    log.error("Error in migration task, task terminated", e);
                }
                if (seriesQueue.peek() == null) {
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
        // adjust threads back to 0 to cancel them
        for (MigrationTask task : adjustThreads(0)) {
            // wait for the cancelled threads to complete
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
        // short-circuit, faster than looking up map
        if (fullyMigrated) return true;

        @Nullable MigrationStatus migrated = seriesStatus.get(vo.getSeriesId());
        if (migrated == null || migrated == MigrationStatus.MIGRATED) {
            // series is a new series, or has been migrated
            return true;
        }

        if (operation == Operation.DELETE) {
            throw new UnsupportedOperationException();
        }
        return false;
    }

    private String stats() {
        long migrated = migratedCount.get();
        long skipped = skippedCount.get();
        long total = totalSeries.get();
        long duration = totalDuration.get();

        long complete = migrated + skipped;
        long seriesLeft = total - complete;
        double avgDuration = (double) duration / complete;
        long timeLeft = (long) (seriesLeft * avgDuration);
        Duration durationLeft = Duration.ofMillis(timeLeft);

        double progress = complete * 100d / total;
        return String.format("[%6.2f%% complete, %d migrated, %d skipped, %d total, %s left, %s ETA]",
                progress, migrated, skipped, total, durationLeft.truncatedTo(ChronoUnit.MINUTES),
                ZonedDateTime.now().plus(durationLeft)
                        .truncatedTo(ChronoUnit.MINUTES)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private class MigrationTask implements Runnable {

        private final CompletableFuture<Void> finished = new CompletableFuture<>();
        private final String name;

        private volatile boolean stopFlag = false;

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

                boolean stopped;
                Integer seriesId;
                while (!(stopped = stopFlag) && (seriesId = seriesQueue.poll()) != null) {
                    MigrationStatus previous = seriesStatus.put(seriesId, MigrationStatus.RUNNING);
                    if (previous != MigrationStatus.NOT_STARTED) {
                        throw new IllegalStateException("Migration should not have stared for series: " + seriesId);
                    }

                    try {
                        new MigrateSeries(seriesId, migrateFrom).migrateSeries();
                    } catch (Exception e) {
                        log.error("Error migrating series {}", seriesId, e);
                        seriesStatus.put(seriesId, MigrationStatus.NOT_STARTED);
                        seriesQueue.add(seriesId);
                    }
                }

                if (stopped) {
                    if (log.isWarnEnabled()) {
                        log.warn("{} Migration task was stopped", stats());
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("{} Migration complete, no more work", stats());
                    }
                }
            } catch (Exception e) {
                finished.completeExceptionally(e);
            } finally {
                finished.complete(null);
            }
        }

        private void stopTask() {
            this.stopFlag = true;
        }

        public CompletableFuture<Void> getFinished() {
            return finished.copy();
        }

        public String getName() {
            return name;
        }

        private class MigrateSeries {

            private final int seriesId;
            private final int batchSize = env.getRequiredProperty("db.migration.batchSize", int.class);

            private Long startFrom;
            private long sampleCount;

            private MigrateSeries(int seriesId, Long startFrom) {
                this.seriesId = seriesId;
                this.startFrom = startFrom;
            }

            private void migrateSeries() {
                DataPointVO point = dataPointDao.getBySeriesId(seriesId);
                if (point == null || !migrationFilter.test(point)) {
                    seriesStatus.put(seriesId, MigrationStatus.SKIPPED);
                    skippedCount.incrementAndGet();
                    if (log.isInfoEnabled()) {
                        log.info("{} Skipped point {} (seriesId={})", stats(), point == null ? null : point.getXid(), seriesId);
                    }
                    return;
                }

                long startTime = System.currentTimeMillis();

                // initial pass at copying data
                copyPointValues(point);
                // copy again as the first run might have taken a long time
                copyPointValues(point);

                // do a final copy inside the compute method so inserts are blocked
                seriesStatus.computeIfPresent(point.getSeriesId(), (k, v) -> {
                    copyPointValues(point);
                    return MigrationStatus.MIGRATED;
                });

                migratedCount.incrementAndGet();
                long duration = System.currentTimeMillis() - startTime;
                totalDuration.addAndGet(duration);

                if (log.isInfoEnabled()) {
                    log.info("{} Migrated point {} (seriesId={}, values={}, duration={}, speed={}/s)",
                            stats(), point.getXid(), seriesId, sampleCount, Duration.ofMillis(duration), String.format("%.2f", sampleCount / (duration / 1000d)));
                }
            }

            private void copyPointValues(DataPointVO point) {
                // stream from secondary (old) db to the primary db (new) in chunks of matching size
                try (var stream = secondary.streamPointValues(point, startFrom, null, null, TimeOrder.ASCENDING, batchSize)) {
                    primary.savePointValues(stream.map(pv -> {
                        startFrom = pv.getTime() + 1;
                        sampleCount++;
                        return new BatchPointValueImpl(point, pv);
                    }), batchSize);
                }
            }
        }
    }
}
