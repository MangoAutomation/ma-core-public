/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DelegatingPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgress;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgressDao;
import com.serotonin.m2m2.vo.DataPointVO;

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

    private final Meter readMeter = new Meter();
    private final Meter writeMeter = new Meter();
    private final Histogram readValuesPerPeriod = new Histogram(new ExponentiallyDecayingReservoir());

    private final ArrayList<MigrationTask> tasks = new ArrayList<>();
    private final Predicate<DataPointVO> dataPointFilter;
    private final DataPointDao dataPointDao;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Instant migrateFrom;
    private final Duration period;
    private final TemporalUnit truncateTo;
    private final TemporalAmount aggregationPeriod;
    private final TemporalAmount aggregationEnd;
    private final TemporalAmount aggregationOverlap;
    private final Set<DataType> aggregationDataTypes;
    private final Clock clock;
    private final Retry retry;
    private final MigrationProgressDao migrationProgressDao;
    private final AtomicLong currentTimestamp = new AtomicLong(Long.MIN_VALUE);
    private final MigrationConfig config;

    private volatile boolean fullyMigrated = false;
    private volatile int readChunkSize;
    private volatile int writeChunkSize;
    private volatile int numTasks;
    private boolean started;
    private boolean terminated;

    private final Object periodicLogFutureMutex = new Object();
    private Future<?> periodicLogFuture;

    public MigrationPointValueDao(PointValueDao destinationPointValueDao,
                                  PointValueDao sourcePointValueDao,
                                  DataPointDao dataPointDao,
                                  ExecutorService executorService,
                                  ScheduledExecutorService scheduledExecutorService,
                                  Clock clock,
                                  MigrationProgressDao migrationProgressDao,
                                  MigrationConfig config) {

        super(destinationPointValueDao, sourcePointValueDao);
        this.dataPointDao = dataPointDao;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.clock = clock.withZone(config.getZone());
        this.migrationProgressDao = migrationProgressDao;
        this.config = config;

        this.dataPointFilter = config.getDataPointFilter();
        this.migrateFrom = config.getMigrateFromTime();
        this.period = config.getMigrationPeriod();
        this.truncateTo = config.getTruncateTo();
        this.aggregationPeriod = config.getAggregationPeriod();
        this.aggregationEnd = config.getAggregationEnd();
        this.aggregationOverlap = config.getAggregationOverlap();
        this.aggregationDataTypes = config.getAggregationDataTypes();

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

        if (config.isAutoStart()) {
            startMigration();
        }
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
        this.readChunkSize = config.getReadChunkSize();
        this.writeChunkSize = config.getWriteChunkSize();
        adjustThreads(config.getThreadCount());

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
            if (terminated) throw new IllegalStateException("Terminated");
            if (!started || fullyMigrated || threadCount == tasks.size()) return List.of();

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

    /**
     * Warning: {@link #seriesStatus} is not thread-safe! Do not attempt to start/stop/restart migration while Mango is running.
     */
    void startMigration() {
        synchronized (tasks) {
            if (terminated) throw new IllegalStateException("Terminated");
            if (started) return;

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

            this.started = true;
            reloadConfig();
        }
    }

    CompletableFuture<Void> migrationFinished() {
        synchronized (tasks) {
            return migrationFinished(tasks);
        }
    }

    CompletableFuture<Void> migrationFinished(Collection<? extends MigrationTask> tasks) {
        return CompletableFuture.allOf(tasks.stream()
                .map(MigrationTask::getFinished)
                .toArray(CompletableFuture[]::new));
    }

    /**
     * Warning: {@link #seriesStatus} is not thread-safe! Do not attempt to start/stop/restart migration while Mango is running.
     * This method is for testing purposes only.
     */
    void reset() {
        synchronized (tasks) {
            // ensure the threads are stopped
            try {
                migrationFinished(adjustThreads(0)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            seriesStatus.clear();
            seriesQueue.clear();
            completedPeriods.set(0L);
            migratedSeries.set(0L);
            skippedSeries.set(0L);
            erroredSeries.set(0L);
            this.fullyMigrated = false;
            this.started = false;
        }
    }

    List<MigrationTask> terminateMigration() {
        synchronized (tasks) {
            if (terminated) return List.of();

            var result = adjustThreads(0);
            this.terminated = true;
            return result;
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
    public void close() throws InterruptedException {
        long closeWaitSeconds = config.getCloseWait().toSeconds();

        // adjust threads back to 0 to cancel them
        for (MigrationTask task : terminateMigration()) {
            // wait for the cancelled threads to complete
            CompletableFuture<Boolean> future = task.getFinished();
            try {
                future.get(closeWaitSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException | CancellationException | ExecutionException e) {
                log.error("Migration task {} failed to stop", task.getName(), e);
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

    String stats() {
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
            // ignore MIN_VALUE which comes from series which haven't completed their initial pass
            if (timestamp != null && timestamp > Long.MIN_VALUE) {
                long timeLeft = clock.millis() - timestamp;
                long periodsLeft = timeLeft / period.toMillis() + 1;
                remainingPeriods = periodsLeft * remaining;
            }
        }

        double avgPointValuesReadPerPeriod = readValuesPerPeriod.getSnapshot().getMean();
        double readRate = readMeter.getOneMinuteRate();
        double writeRate = writeMeter.getOneMinuteRate();

        long totalPeriods = remainingPeriods + completedPeriods;
        double progress = remainingPeriods == 0L ? 100D: completedPeriods * 100d / totalPeriods;

        double pointValuesRemaining = remainingPeriods * avgPointValuesReadPerPeriod;
        String eta = "—";
        String timeLeft = "∞";
        if (readRate > 0D) {
            long secondsLeft = (long) (pointValuesRemaining / readRate);
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

        return String.format("[%6.2f%% complete, %d/%d periods, %.1f reads/s, %.1f writes/s, %.1f reads/period, position %s, ETA %s (%s), %d/%d/%d failed/retried/success, %d threads]",
                progress,
                completedPeriods,
                totalPeriods,
                readRate,
                writeRate,
                avgPointValuesReadPerPeriod,
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

    Instant getMigrateFrom() {
        return migrateFrom;
    }

    PointValueDao getSource() {
        return secondary;
    }

    PointValueDao getDestination() {
        return primary;
    }

    Duration getPeriod() {
        return period;
    }

    TemporalUnit getTruncateTo() {
        return truncateTo;
    }

    TemporalAmount getAggregationPeriod() {
        return aggregationPeriod;
    }

    TemporalAmount getAggregationEnd() {
        return aggregationEnd;
    }

    public TemporalAmount getAggregationOverlap() {
        return aggregationOverlap;
    }

    Set<DataType> getAggregationDataTypes() {
        return aggregationDataTypes;
    }

    Clock getClock() {
        return clock;
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
     * @param readCount number of point values read
     * @param writeCount number of point values written
     * @param duration time taken for this period
     */
    void updateProgress(MigrationSeries series, long readCount, long writeCount, Duration duration) {
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
                // does 2 periods on last iteration
                completedPeriods.addAndGet(2L);
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

        currentTimestamp.updateAndGet(v -> Math.max(series.getTimestamp(), v));
        readValuesPerPeriod.update(readCount);
        readMeter.mark(readCount);
        writeMeter.mark(writeCount);

        if (log.isTraceEnabled()) {
            double valuesPerSecond = writeCount / (duration.toMillis() / 1000d);
            log.trace("{} Completed period for {} (status={}, values={}, duration={}, speed={} values/s)",
                    stats(), series, series.getStatus(), writeCount, duration,
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
