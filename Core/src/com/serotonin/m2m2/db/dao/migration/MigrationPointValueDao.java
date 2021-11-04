/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;
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
import com.serotonin.timer.AbstractTimer;
import com.serotonin.util.properties.MangoConfigurationWatcher.MangoConfigurationReloadedEvent;

public class MigrationPointValueDao extends DelegatingPointValueDao implements AutoCloseable {

    /**
     * Separate log file is configured for this logger
     */
    private final Logger log = LoggerFactory.getLogger("pointValueMigration");
    /**
     * Map key is series id
     */
    private final Map<Integer, MigrationSeries> seriesStatus = new HashMap<>();
    private final Queue<MigrationSeries> seriesQueue = new PriorityBlockingQueue<>(128,
            Comparator.nullsFirst(Comparator.comparingLong(MigrationSeries::getTimestamp)));

    private final AtomicLong completedPeriods = new AtomicLong();
    private final AtomicLong migratedSeries = new AtomicLong();
    private final AtomicLong skippedSeries = new AtomicLong();
    private final AtomicLong erroredSeries = new AtomicLong();

    private final SynchronizedSummaryStatistics writeSpeed = new SynchronizedSummaryStatistics();
    private final SynchronizedSummaryStatistics totalValues = new SynchronizedSummaryStatistics();

    private final ArrayList<MigrationTask> tasks = new ArrayList<>();
    private final Predicate<DataPointVO> migrationFilter;
    private final DataPointDao dataPointDao;
    private final Environment env;
    private final ExecutorService executorService;
    private final ConfigurableApplicationContext context;
    private final Long migrateFrom;
    private final long period;
    private final AbstractTimer timer;

    private volatile boolean fullyMigrated = false;
    private volatile int readChunkSize;
    private volatile int writeChunkSize;
    private volatile int numTasks;
    private boolean terminated;

    public enum MigrationStatus {
        NOT_STARTED,
        RUNNING,
        MIGRATED,
        SKIPPED,
        ERROR
    }

    public MigrationPointValueDao(PointValueDao primary,
                                  PointValueDao secondary,
                                  DataPointDao dataPointDao,
                                  Predicate<DataPointVO> migrationFilter,
                                  Environment env,
                                  ExecutorService executorService,
                                  ConfigurableApplicationContext context,
                                  AbstractTimer timer) {
        super(primary, secondary);
        this.dataPointDao = dataPointDao;
        this.migrationFilter = migrationFilter;
        this.env = env;
        this.executorService = executorService;
        this.context = context;
        this.timer = timer;

        String fromStr = env.getProperty("db.migration.fromDate");
        if (fromStr != null) {
            this.migrateFrom = ZonedDateTime.parse(fromStr).toInstant().toEpochMilli();
        } else {
            this.migrateFrom = null;
        }

        long period = env.getProperty("db.migration.period", Long.class, 1L);
        TimeUnit periodUnit = env.getProperty("db.migration.periodUnit", TimeUnit.class, TimeUnit.DAYS);
        this.period = periodUnit.toMillis(period);
    }

    @PostConstruct
    private void postConstruct() {
        try (var stream = dataPointDao.streamSeriesIds()) {
            stream.mapToObj(MigrationSeries::new).forEach(lock -> {
                seriesStatus.put(lock.seriesId, lock);
                seriesQueue.add(lock);
            });
        }
        context.addApplicationListener((ApplicationListener<MangoConfigurationReloadedEvent>) e -> loadProperties());
        loadProperties();
    }

    private void loadProperties() {
        adjustThreads(env.getProperty("db.migration.threadCount", int.class, Math.max(1, Runtime.getRuntime().availableProcessors() / 4)));
        this.readChunkSize = env.getProperty("db.migration.readChunkSize", int.class, 10000);
        this.writeChunkSize = env.getProperty("db.migration.writeChunkSize", int.class, 10000);
    }

    private List<MigrationTask> adjustThreads(int threadCount) {
        synchronized (tasks) {
            if (terminated) throw new IllegalStateException();
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
                    removeTask(task);
                });
                executorService.execute(task);
            }

            while (tasks.size() > threadCount) {
                MigrationTask task = tasks.remove(tasks.size() - 1);
                task.stopTask();
                stoppedTasks.add(task);
            }

            writeSpeed.clear();
            this.numTasks = tasks.size();
            return stoppedTasks;
        }
    }

    private List<MigrationTask> terminate() {
        synchronized (tasks) {
            var stopped = this.adjustThreads(0);
            this.terminated = true;
            return stopped;
        }
    }

    private void removeTask(MigrationTask task) {
        synchronized (tasks) {
            tasks.remove(task);
            if (tasks.isEmpty() && seriesQueue.isEmpty()) {
                this.fullyMigrated = true;
            }
            writeSpeed.clear();
            this.numTasks = tasks.size();
        }
    }

    @Override
    public void close() throws Exception {
        long closeWait = env.getProperty("db.migration.closeWait", Long.class, 1L);
        TimeUnit closeWaitUnit = env.getProperty("db.migration.closeWaitUnit", TimeUnit.class, TimeUnit.MINUTES);

        // adjust threads back to 0 to cancel them
        for (MigrationTask task : terminate()) {
            // wait for the cancelled threads to complete
            task.getFinished().get(closeWait, closeWaitUnit);
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

        @Nullable MigrationPointValueDao.MigrationSeries series = seriesStatus.get(vo.getSeriesId());
        if (series == null || series.isMigrated()) {
            // series is a new series, or has been migrated
            return true;
        }

        if (operation == Operation.DELETE) {
            throw new UnsupportedOperationException();
        }
        return false;
    }

    private String stats() {
        long migrated = migratedSeries.get();
        long skipped = skippedSeries.get();
        long errored = erroredSeries.get();
        long finished = migrated + skipped + errored;
        long remaining = seriesStatus.size() - finished;
        long completedPeriods = this.completedPeriods.get();

        long remainingPeriods = 0;
        MigrationSeries next = seriesQueue.peek();
        if (next != null) {
            Long timestamp = next.getTimestamp();
            if (timestamp != null) {
                long timeLeft = timer.currentTimeMillis() - timestamp;
                long periodsLeft = timeLeft / period + 1;
                remainingPeriods = periodsLeft * remaining;
            }
        }

        double avgPointValuesPerPeriod = totalValues.getMean();
        double avgSpeed = writeSpeed.getMean() * numTasks;
        if (Double.isNaN(avgSpeed)) {
            avgSpeed = 0D;
        }

        long totalPeriods = remainingPeriods + completedPeriods;
        double progress = remainingPeriods == 0L ? 100D: completedPeriods * 100d / totalPeriods;

        double pointValuesRemaining = remainingPeriods * avgPointValuesPerPeriod;
        Duration durationLeft = null;
        if (avgSpeed > 0D) {
            long secondsLeft = (long) (pointValuesRemaining / avgSpeed);
            durationLeft = Duration.ofSeconds(secondsLeft);
        }

        return String.format("[%6.2f%% complete, %d/%d periods, %.1f values/s, %.1f values/period, %s ETA (%s)]",
                progress,
                completedPeriods,
                totalPeriods,
                avgSpeed,
                avgPointValuesPerPeriod,
                durationLeft != null ?
                ZonedDateTime.now().plus(durationLeft)
                        .truncatedTo(ChronoUnit.MINUTES)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—",
                durationLeft != null ? durationLeft.truncatedTo(ChronoUnit.MINUTES) : "∞");
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
                    log.info("Migration task starting");
                }

                boolean stopped;
                MigrationSeries series;
                while (!(stopped = stopFlag) && (series = seriesQueue.poll()) != null) {
                    if (series.run() == MigrationStatus.RUNNING) {
                        seriesQueue.add(series);
                    }
                }

                if (stopped) {
                    if (log.isWarnEnabled()) {
                        log.warn("Migration task was stopped");
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Migration complete, no more work");
                    }
                }
            } catch (Exception e) {
                finished.completeExceptionally(e);
            } catch (Throwable t) {
                finished.completeExceptionally(t);
                throw t;
            } finally {
                finished.complete(null);
            }
        }

        private void stopTask() {
            this.stopFlag = true;
        }

        private CompletableFuture<Void> getFinished() {
            return finished.copy();
        }

        private String getName() {
            return name;
        }
    }

    private class MigrationSeries {
        private final int seriesId;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        private MigrationStatus status = MigrationStatus.NOT_STARTED;
        private DataPointVO point;
        private volatile long timestamp;
        private long sampleCount;

        private MigrationSeries(int seriesId) {
            this.seriesId = seriesId;
        }

        private synchronized MigrationStatus run() {
            try {
                if (point == null) {
                    initialPass();
                } else {
                    migrateNextPeriod();
                }
            } catch (Exception e) {
                erroredSeries.incrementAndGet();
                this.status = MigrationStatus.ERROR;
                log.error("Error migrating series {}, migration aborted for series", seriesId, e);
            } catch (Throwable t) {
                erroredSeries.incrementAndGet();
                this.status = MigrationStatus.ERROR;
                throw t;
            }
            return status;
        }

        private void initialPass() {
            this.point = dataPointDao.getBySeriesId(seriesId);
            if (point == null || !migrationFilter.test(point)) {
                this.status = MigrationStatus.SKIPPED;
                skippedSeries.incrementAndGet();
                if (log.isInfoEnabled()) {
                    log.info("{} Skipped point {} (seriesId={})", stats(), point == null ? null : point.getXid(), seriesId);
                }
            } else {
                lock.writeLock().lock();
                try {
                    Optional<Long> inception = secondary.getInceptionDate(point);
                    if (inception.isPresent()) {
                        this.status = MigrationStatus.RUNNING;
                        long timestamp = inception.get();
                        if (migrateFrom != null) {
                            timestamp = Math.max(timestamp, migrateFrom);
                        }
                        this.timestamp = timestamp;
                    } else {
                        // no data
                        this.status = MigrationStatus.MIGRATED;
                        migratedSeries.incrementAndGet();
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        private void migrateNextPeriod() {
            long startTime = System.currentTimeMillis();

            long timestamp = this.timestamp;
            long from = timestamp - timestamp % period;
            long to = from + period;
            if (migrateFrom != null) {
                from = Math.max(from, migrateFrom);
            }
            this.sampleCount = 0;

            long currentTime = timer.currentTimeMillis();
            if (currentTime < to) {
                // close out series, do the final copy while holding the write-lock so inserts are blocked
                lock.writeLock().lock();
                try {
                    // copy an extra period in case the current time rolls over into the next period
                    copyPointValues(from, to + period);
                    this.status = MigrationStatus.MIGRATED;
                    migratedSeries.incrementAndGet();
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                copyPointValues(from, to);
            }

            long duration = System.currentTimeMillis() - startTime;
            double valuesPerSecond = sampleCount / (duration / 1000d);

            writeSpeed.addValue(valuesPerSecond);
            totalValues.addValue(sampleCount);

            if (log.isDebugEnabled()) {
                log.debug("{} Completed period for {}, from {} to {} (seriesId={}, values={}, duration={}, speed={} values/s)",
                        stats(), point.getXid(), Instant.ofEpochMilli(from), Instant.ofEpochMilli(to),
                        seriesId, sampleCount, Duration.ofMillis(duration),
                        String.format("%.1f", valuesPerSecond));
            }
        }

        private void copyPointValues(long from, long to) {
            this.sampleCount = 0;
            // stream from secondary (old) db to the primary db (new) in chunks of matching size
            try (var stream = secondary.streamPointValues(point, from, to, null, TimeOrder.ASCENDING, readChunkSize)) {
                primary.savePointValues(stream.map(pv -> {
                    this.sampleCount++;
                    return new BatchPointValueImpl(point, pv);
                }), writeChunkSize);
            }
            this.timestamp = to;
            completedPeriods.incrementAndGet();
        }

        private Long getTimestamp() {
            return timestamp;
        }

        private boolean isMigrated() {
            lock.readLock().lock();
            try {
                return status == MigrationStatus.MIGRATED;
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
