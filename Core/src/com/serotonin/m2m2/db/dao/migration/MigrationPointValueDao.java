/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DelegatingPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.migration.MigrationProgressDao.MigrationProgress;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.util.properties.MangoConfigurationWatcher.MangoConfigurationReloadedEvent;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.Retry.Metrics;
import io.github.resilience4j.retry.RetryConfig;

public class MigrationPointValueDao extends DelegatingPointValueDao implements AutoCloseable {

    private final Logger log = LoggerFactory.getLogger(MigrationPointValueDao.class);

    /**
     * Map key is series id
     */
    private final Map<Integer, MigrationSeries> seriesStatus = new HashMap<>();
    private final Queue<MigrationSeries> seriesQueue = new PriorityBlockingQueue<>(1024,
            Comparator.nullsFirst(Comparator.comparingLong(MigrationSeries::getTimestamp)));

    private final AtomicLong completedPeriods = new AtomicLong();
    private final AtomicLong migratedSeries = new AtomicLong();
    private final AtomicLong skippedSeries = new AtomicLong();
    private final AtomicLong erroredSeries = new AtomicLong();

    private final Meter writeMeter = new Meter();
    private final Histogram valuesPerPeriod = new Histogram(new ExponentiallyDecayingReservoir());

    private final ArrayList<MigrationTask> tasks = new ArrayList<>();
    private final Predicate<DataPointVO> migrationFilter;
    private final DataPointDao dataPointDao;
    private final Environment env;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ConfigurableApplicationContext context;
    private final Long migrateFrom;
    private final long period;
    private final AbstractTimer timer;
    private final Retry retry;
    private final MigrationProgressDao migrationProgressDao;
    private final AtomicLong currentTimestamp = new AtomicLong(Long.MIN_VALUE);

    private volatile boolean fullyMigrated = false;
    private volatile int readChunkSize;
    private volatile int writeChunkSize;
    private volatile int numTasks;
    private boolean terminated;

    private final Object periodicLogFutureMutex = new Object();
    private Future<?> periodicLogFuture;

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
                                  ScheduledExecutorService scheduledExecutorService,
                                  ConfigurableApplicationContext context,
                                  AbstractTimer timer,
                                  MigrationProgressDao migrationProgressDao) {
        super(primary, secondary);
        this.dataPointDao = dataPointDao;
        this.migrationFilter = migrationFilter;
        this.env = env;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.context = context;
        this.timer = timer;
        this.migrationProgressDao = migrationProgressDao;

        String fromStr = env.getProperty("db.migration.fromDate");
        if (fromStr != null) {
            this.migrateFrom = ZonedDateTime.parse(fromStr).toInstant().toEpochMilli();
        } else {
            this.migrateFrom = null;
        }

        long period = env.getProperty("db.migration.period", Long.class, 1L);
        TimeUnit periodUnit = env.getProperty("db.migration.periodUnit", TimeUnit.class, TimeUnit.DAYS);
        this.period = periodUnit.toMillis(period);

        int maxAttempts = env.getProperty("db.migration.maxAttempts", int.class, 5);
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .failAfterMaxAttempts(true)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(500, 2.0D, 0.2D, 60_000))
                .build();
        this.retry = Retry.of("migratePeriod", retryConfig);

        this.retry.getEventPublisher()
                .onRetry(event -> log.debug("Retry, waiting {} until attempt {}.", event.getWaitInterval(), event.getNumberOfRetryAttempts(), event.getLastThrowable()))
                .onError(event -> log.debug("Recorded a failed retry attempt. Number of retry attempts: {}. Giving up.", event.getNumberOfRetryAttempts(), event.getLastThrowable()));
    }

    @PostConstruct
    private void postConstruct() {
        if (env.getProperty("db.migration.startNewMigration", boolean.class, false)) {
            migrationProgressDao.deleteAll();
        }

        if (migrationProgressDao.count() > 0) {
            // migration in progress, restore from DB
            try (var stream = migrationProgressDao.stream()) {
                stream.map(progress -> new MigrationSeries(progress.getSeriesId(), progress.getStatus(), progress.getTimestamp()))
                        .forEach(this::addMigration);
            }
        } else {
            // start a new migration, get all points and insert progress items for them
            try (var stream = dataPointDao.streamSeriesIds()) {
                Stream<MigrationProgress> progressStream = stream.mapToObj(MigrationSeries::new)
                        .peek(this::addMigration)
                        .map(migration -> new MigrationProgress(migration.seriesId, migration.status, migration.timestamp));
                migrationProgressDao.bulkInsert(progressStream);
            }
        }

        context.addApplicationListener((ApplicationListener<MangoConfigurationReloadedEvent>) e -> loadProperties());
        loadProperties();
    }

    private void addMigration(MigrationSeries migration) {
        seriesStatus.put(migration.seriesId, migration);
        if (migration.status == MigrationStatus.NOT_STARTED || migration.status == MigrationStatus.RUNNING) {
            seriesQueue.add(migration);
        }
    }

    private void loadProperties() {
        adjustThreads(env.getProperty("db.migration.threadCount", int.class, Math.max(1, Runtime.getRuntime().availableProcessors() / 4)));
        this.readChunkSize = env.getProperty("db.migration.readChunkSize", int.class, 10000);
        this.writeChunkSize = env.getProperty("db.migration.writeChunkSize", int.class, 10000);

        synchronized (periodicLogFutureMutex) {
            if (periodicLogFuture != null) {
                periodicLogFuture.cancel(false);
                this.periodicLogFuture = null;
            }
            int logPeriod = env.getProperty("db.migration.logPeriodSeconds", int.class, 60);
            if (logPeriod > 0) {
                this.periodicLogFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
                    if (log.isInfoEnabled() && numTasks > 0) {
                        log.info("{}", stats());
                    }
                }, 0, logPeriod, TimeUnit.SECONDS);
            }
        }
    }

    private List<MigrationTask> adjustThreads(int threadCount) {
        synchronized (tasks) {
            if (terminated) throw new IllegalStateException();
            if (fullyMigrated || threadCount == tasks.size()) return Collections.emptyList();

            log.info("Adjusting migration threads from {} to {}", tasks.size(), threadCount);
            List<MigrationTask> stoppedTasks = new ArrayList<>();

            while (tasks.size() < threadCount) {
                MigrationTask task = new MigrationTask(seriesQueue, tasks.size() + 1);
                tasks.add(task);
                this.numTasks = tasks.size();
                task.getFinished().whenComplete((stopped, e) -> removeTask(task));
                executorService.execute(task);
            }

            while (tasks.size() > threadCount) {
                MigrationTask task = tasks.remove(tasks.size() - 1);
                this.numTasks = tasks.size();
                task.stopTask();
                stoppedTasks.add(task);
            }

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
            this.numTasks = tasks.size();
            if (tasks.isEmpty() && seriesQueue.isEmpty()) {
                this.fullyMigrated = true;
            }
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

    @Override
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

        double avgPointValuesPerPeriod = valuesPerPeriod.getSnapshot().getMean();
        double writeRate = writeMeter.getOneMinuteRate();

        long totalPeriods = remainingPeriods + completedPeriods;
        double progress = remainingPeriods == 0L ? 100D: completedPeriods * 100d / totalPeriods;

        double pointValuesRemaining = remainingPeriods * avgPointValuesPerPeriod;
        String eta = "—";
        String timeLeft = "∞";
        if (writeRate > 0D) {
            long secondsLeft = (long) (pointValuesRemaining / writeRate);
            Duration durationLeft = Duration.ofSeconds(secondsLeft);
            eta = ZonedDateTime.now()
                    .plus(durationLeft)
                    .truncatedTo(ChronoUnit.MINUTES)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            timeLeft = durationLeft.truncatedTo(ChronoUnit.MINUTES).toString();
        }

        String currentTimeFormatted = "—";
        long currentTimestamp = this.currentTimestamp.get();
        if (currentTimestamp > Long.MIN_VALUE) {
            currentTimeFormatted = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimestamp), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        Metrics retryMetrics = retry.getMetrics();

        return String.format("[%6.2f%% complete, %d/%d periods, %.1f values/s, %.1f values/period, position %s, ETA %s (%s), %d/%d/%d failed/retried/success, %d threads]",
                progress,
                completedPeriods,
                totalPeriods,
                writeRate,
                avgPointValuesPerPeriod,
                currentTimeFormatted,
                eta,
                timeLeft,
                retryMetrics.getNumberOfFailedCallsWithRetryAttempt() + retryMetrics.getNumberOfFailedCallsWithoutRetryAttempt(),
                retryMetrics.getNumberOfSuccessfulCallsWithRetryAttempt(),
                retryMetrics.getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                numTasks);
    }

    class MigrationSeries {
        private final int seriesId;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        private MigrationStatus status;
        private DataPointVO point;
        private volatile long timestamp;
        private long sampleCount;
        private boolean initialPassComplete;

        private final Runnable initialPassRetry = Retry.decorateRunnable(retry, this::initialPass);
        private final Runnable migrateNextPeriodRetry = Retry.decorateRunnable(retry, this::migrateNextPeriod);

        private MigrationSeries(int seriesId) {
            this(seriesId, MigrationStatus.NOT_STARTED, 0L);
        }

        private MigrationSeries(int seriesId, MigrationStatus status, long timestamp) {
            this.seriesId = seriesId;
            this.status = status;
            this.timestamp = timestamp;
        }

        synchronized MigrationStatus run() {
            try {
                if (!initialPassComplete) {
                    initialPassRetry.run();
                } else {
                    migrateNextPeriodRetry.run();
                }
            } catch (Exception e) {
                erroredSeries.incrementAndGet();
                this.status = MigrationStatus.ERROR;
                log.error("Error migrating period, migration aborted for point {} (seriesId={})", point != null ? point.getXid() : null, seriesId, e);
            } catch (Throwable t) {
                erroredSeries.incrementAndGet();
                this.status = MigrationStatus.ERROR;
                throw t;
            }

            try {
                migrationProgressDao.update(new MigrationProgress(seriesId, status, timestamp));
            } catch (Exception e) {
                log.warn("Failed to save migration progress to database for point {} (seriesId={})", point != null ? point.getXid() : null, seriesId, e);
            }

            return status;
        }

        private void initialPass() {
            this.point = dataPointDao.getBySeriesId(seriesId);
            if (point == null || !migrationFilter.test(point)) {
                this.status = MigrationStatus.SKIPPED;
                skippedSeries.incrementAndGet();
                if (log.isInfoEnabled()) {
                    log.info("{} Skipped point {} (seriesId={})", stats(), point != null ? point.getXid() : null, seriesId);
                }
            } else if (this.status == MigrationStatus.NOT_STARTED) {
                // only get the initial timestamp if migration was not started yet, otherwise we already have retrieved it from the database
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
            this.initialPassComplete = true;
        }

        private void migrateNextPeriod() {
            long startTime = System.currentTimeMillis();

            long timestamp = this.timestamp;
            long from = timestamp - timestamp % period;
            long to = from + period;
            if (migrateFrom != null) {
                from = Math.max(from, migrateFrom);
            }
            long fromFinal = from;
            currentTimestamp.updateAndGet(v -> Math.max(fromFinal, v));
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

            valuesPerPeriod.update(sampleCount);
            writeMeter.mark(sampleCount);

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

    @Override
    public void setRetentionPolicy(Period period) {
        throw new UnsupportedOperationException();
    }
}
