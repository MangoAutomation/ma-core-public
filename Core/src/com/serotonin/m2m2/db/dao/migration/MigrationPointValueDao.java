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
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DelegatingPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.migration.MigrationProgressDao.MigrationProgress;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.timer.AbstractTimer;

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
    private final Predicate<DataPointVO> dataPointFilter;
    private final DataPointDao dataPointDao;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Long migrateFrom;
    private final long period;
    private final AbstractTimer timer;
    private final Retry retry;
    private final MigrationProgressDao migrationProgressDao;
    private final AtomicLong currentTimestamp = new AtomicLong(Long.MAX_VALUE);
    private final MigrationConfig config;

    private volatile boolean fullyMigrated = false;
    private volatile int readChunkSize;
    private volatile int writeChunkSize;
    private volatile int numTasks;
    private boolean terminated;

    private final Object periodicLogFutureMutex = new Object();
    private Future<?> periodicLogFuture;

    public MigrationPointValueDao(PointValueDao primary,
                                  PointValueDao secondary,
                                  DataPointDao dataPointDao,
                                  ExecutorService executorService,
                                  ScheduledExecutorService scheduledExecutorService,
                                  AbstractTimer timer,
                                  MigrationProgressDao migrationProgressDao,
                                  MigrationConfig config) {

        super(primary, secondary);
        this.dataPointDao = dataPointDao;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.timer = timer;
        this.migrationProgressDao = migrationProgressDao;
        this.config = config;

        this.dataPointFilter = config.getDataPointFilter();
        Instant migrateFromTime = config.getMigrateFromTime();
        this.migrateFrom = migrateFromTime == null ? null : migrateFromTime.toEpochMilli();
        this.period = config.getMigrationPeriod().toMillis();

        int maxAttempts = config.getMaxAttempts();
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
        if (config.isStartNewMigration()) {
            migrationProgressDao.deleteAll();
        }

        if (migrationProgressDao.count() > 0) {
            // migration in progress, restore from DB
            try (var stream = migrationProgressDao.stream()) {
                stream.map(progress -> new MigrationSeries(this, progress.getSeriesId(), progress.getStatus(), progress.getTimestamp()))
                        .forEach(this::addMigration);
            }
        } else {
            // start a new migration, get all points and insert progress items for them
            try (var stream = dataPointDao.streamSeriesIds()) {
                Stream<MigrationProgress> progressStream = stream.mapToObj(seriesId -> new MigrationSeries(this, seriesId))
                        .peek(this::addMigration)
                        .map(MigrationSeries::getMigrationProgress);
                migrationProgressDao.bulkInsert(progressStream);
            }
        }

        reloadConfig();
    }

    private void addMigration(MigrationSeries migration) {
        seriesStatus.put(migration.getSeriesId(), migration);

        switch (migration.getStatus()) {
            case NOT_STARTED:
            case RUNNING:
                seriesQueue.add(migration);
                break;
        }
    }

    public void reloadConfig() {
        adjustThreads(config.getThreadCount());
        this.readChunkSize = config.getReadChunkSize();
        this.writeChunkSize = config.getWriteChunkSize();

        synchronized (periodicLogFutureMutex) {
            if (periodicLogFuture != null) {
                periodicLogFuture.cancel(false);
                this.periodicLogFuture = null;
            }
            int logPeriod = config.getLogPeriodSeconds();
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
        long seconds = config.getCloseWait().toSeconds();

        // adjust threads back to 0 to cancel them
        for (MigrationTask task : terminate()) {
            // wait for the cancelled threads to complete
            task.getFinished().get(seconds, TimeUnit.SECONDS);
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

        @Nullable MigrationSeries series = seriesStatus.get(vo.getSeriesId());
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
        if (currentTimestamp < Long.MAX_VALUE) {
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

    @Override
    public void setRetentionPolicy(Period period) {
        throw new UnsupportedOperationException();
    }

    Predicate<DataPointVO> getDataPointFilter() {
        return dataPointFilter;
    }

    DataPointDao getDataPointDao() {
        return dataPointDao;
    }

    Retry getRetry() {
        return retry;
    }

    Long getMigrateFrom() {
        return migrateFrom;
    }

    PointValueDao getSource() {
        return secondary;
    }

    PointValueDao getDestination() {
        return primary;
    }

    long getPeriod() {
        return period;
    }

    AbstractTimer getTimer() {
        return timer;
    }

    int getReadChunkSize() {
        return readChunkSize;
    }

    int getWriteChunkSize() {
        return writeChunkSize;
    }

    /**
     * Called after each iteration of a series migration.
     *
     * @param series series which is being migrated
     * @param sampleCount number of samples migrated
     * @param duration time taken for this period
     */
    void updateProgress(MigrationSeries series, long sampleCount, Duration duration) {
        switch (series.getStatus()) {
            case INITIAL_PASS_COMPLETE:
                break;
            case NO_DATA:
                migratedSeries.incrementAndGet();
                break;
            case RUNNING:
                completedPeriods.incrementAndGet();
                break;
            case MIGRATED:
                completedPeriods.incrementAndGet();
                migratedSeries.incrementAndGet();
                break;
            case SKIPPED:
                skippedSeries.incrementAndGet();
                break;
            case ERROR:
                erroredSeries.incrementAndGet();
                break;
            default: throw new IllegalStateException("Incorrect status: " + series.getStatus());
        }

        currentTimestamp.updateAndGet(v -> Math.min(series.getTimestamp(), v));
        valuesPerPeriod.update(sampleCount);
        writeMeter.mark(sampleCount);

        if (log.isTraceEnabled()) {
            double valuesPerSecond = sampleCount / (duration.toMillis() / 1000d);
            log.trace("{} Completed period for {} (status={}, values={}, duration={}, speed={} values/s)",
                    stats(), series, series.getStatus(), sampleCount, duration,
                    String.format("%.1f", valuesPerSecond));
        }

        try {
            migrationProgressDao.update(series.getMigrationProgress());
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to save progress to database for {}", series, e);
            }
        }
    }
}
