/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.infiniteautomation.mango.pointvalue.generator.BrownianPointValueGenerator;
import com.infiniteautomation.mango.pointvalue.generator.LinearPointValueGenerator;
import com.infiniteautomation.mango.pointvalue.generator.PointValueGenerator;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockAggregateDao;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockPointValueDao;
import com.serotonin.m2m2.SimulationTimerProvider;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgressDao;
import com.serotonin.m2m2.db.dao.pointvalue.AggregateValue;
import com.serotonin.m2m2.db.dao.pointvalue.NumericAggregate;
import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.stats.SeriesValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.timer.SimulationTimer;

/**
 * @author Jared Wiltshire
 */
public class MigrationPointValueDaoTest extends MangoTestBase {

    MigrationPointValueDao migrationPointValueDao;
    TestMigrationConfig migrationConfig;
    MockPointValueDao source;
    MockPointValueDao destination;
    SimulationTimer timer;

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setTimer(new SimulationTimerProvider(new SimulationTimer(ZoneOffset.UTC)));
        return lifecycle;
    }

    @Override
    public void before() {
        super.before();
        useMigrationConfig(new TestMigrationConfig());
    }

    private void useMigrationConfig(TestMigrationConfig config) {
        ApplicationContext context = MangoTestBase.lifecycle.getRuntimeContext();
        this.migrationConfig = config;

        // keep a reference to the new timer, so we can set the time on the instance passed to migration dao
        this.timer = context.getBean(SimulationTimer.class).withZone(config.getZone());

        this.source = new MockPointValueDao();
        this.destination = new MockPointValueDao(dao -> new MockAggregateDao(dao, timer, Duration.ofMinutes(15L), Duration.ofDays(1L)));

        this.migrationPointValueDao = new MigrationPointValueDao(destination, source,
                context.getBean(DataPointDao.class),
                context.getBean(ExecutorService.class),
                context.getBean(ScheduledExecutorService.class),
                timer,
                context.getBean(MigrationProgressDao.class),
                config);
    }

    @Test
    public void singleValue() throws ExecutionException, InterruptedException, TimeoutException {
        var dataSource = createMockDataSource();
        var point = createMockDataPoint(dataSource, new MockPointLocatorVO());

        Instant from = Instant.ofEpochMilli(0L);
        var sourceValues = List.of(new PointValueTime(0.0D, from.toEpochMilli()));
        var batchInsertValues = sourceValues.stream().map(v -> new BatchPointValueImpl<>(point, v))
                .collect(Collectors.toList());
        source.savePointValues(batchInsertValues.stream());

        migrationPointValueDao.startMigration();
        migrationPointValueDao.migrationFinished().get(30, TimeUnit.SECONDS);

        List<IdPointValueTime> destinationValues;
        try (var stream = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)) {
            destinationValues = stream.collect(Collectors.toList());
        }

        assertEquals(sourceValues.size(), destinationValues.size());
        for (int i = 0; i < sourceValues.size(); i++) {
            var sourceValue = sourceValues.get(i);
            var destinationValue = destinationValues.get(i);
            assertEquals(point.getSeriesId(), destinationValue.getSeriesId());
            assertEquals(sourceValue.getTime(), destinationValue.getTime());
            assertEquals(sourceValue.getValue(), destinationValue.getValue());
        }
    }

    @Test
    public void generatedBrownian() throws ExecutionException, InterruptedException, TimeoutException {
        var dataSource = createMockDataSource();
        var point = createMockDataPoint(dataSource, new MockPointLocatorVO());

        TemporalAmount testDuration = Duration.ofDays(30);
        ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC);
        ZonedDateTime to = from.plus(testDuration);
        Duration period = Duration.ofHours(1L);
        long expectedSamples = Duration.between(from, to).dividedBy(period);

        // migration stops at the current time
        timer.setStartTime(to.toInstant().toEpochMilli());

        BrownianPointValueGenerator generator = new BrownianPointValueGenerator(from.toInstant(), to.toInstant(), period);
        source.savePointValues(generator.apply(point));
        // sanity check
        assertEquals(expectedSamples, source.dateRangeCount(point, null, null));

        migrationPointValueDao.startMigration();
        migrationPointValueDao.migrationFinished().get(30, TimeUnit.SECONDS);

        List<IdPointValueTime> destinationValues;
        try (var stream = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)) {
            destinationValues = stream.collect(Collectors.toList());
        }

        assertEquals(expectedSamples, destinationValues.size());
        for (int i = 0; i < expectedSamples; i++) {
            var destinationValue = destinationValues.get(i);
            assertEquals(point.getSeriesId(), destinationValue.getSeriesId());
            assertEquals(from.plus(period.multipliedBy(i)).toInstant().toEpochMilli(), destinationValue.getTime());
        }
    }

    @Test
    public void multipleDataPoints() throws ExecutionException, InterruptedException, TimeoutException {
        var dataSource = createMockDataSource();
        var points = createMockDataPoints(dataSource, 100);

        TemporalAmount testDuration = Period.ofMonths(6);
        ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC);
        ZonedDateTime to = from.plus(testDuration);
        Duration period = Duration.ofHours(1L);
        long expectedSamples = Duration.between(from, to).dividedBy(period);

        // migration stops at the current time
        timer.setStartTime(to.toInstant().toEpochMilli());

        BrownianPointValueGenerator generator = new BrownianPointValueGenerator(from.toInstant(), to.toInstant(), period);
        for (var point : points) {
            source.savePointValues(generator.apply(point));
            // sanity check
            assertEquals(expectedSamples, source.dateRangeCount(point, null, null));
        }

        migrationPointValueDao.startMigration();
        migrationPointValueDao.migrationFinished().get(30, TimeUnit.SECONDS);

        for (var point : points) {
            List<IdPointValueTime> destinationValues;
            try (var stream = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)) {
                destinationValues = stream.collect(Collectors.toList());
            }
            assertEquals(expectedSamples, destinationValues.size());
            for (int i = 0; i < expectedSamples; i++) {
                var destinationValue = destinationValues.get(i);
                assertEquals(point.getSeriesId(), destinationValue.getSeriesId());
                assertEquals(from.plus(period.multipliedBy(i)).toInstant().toEpochMilli(), destinationValue.getTime());
            }
        }
    }

    /**
     * Tests migration of raw numeric values to 15-minute aggregates, for 1 day, from 2020-01-01 to 2020-01-02.
     * Verifies the number of aggregate samples in the destination database and the average value for each 15-minute period.
     */
    @Test
    public void downsampledAggregate() throws ExecutionException, InterruptedException, TimeoutException {
        Duration aggregationPeriod = Duration.ofMinutes(15);
        TestMigrationConfig config = new TestMigrationConfig();
        config.setAggregationPeriod(aggregationPeriod);
        useMigrationConfig(config);

        var dataSource = createMockDataSource();
        var point = createMockDataPoint(dataSource, new MockPointLocatorVO());

        TemporalAmount testDuration = Duration.ofDays(1);
        ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC);
        ZonedDateTime to = from.plus(testDuration);
        Duration period = Duration.ofSeconds(5L);
        long inputExpectedSamples = Duration.between(from, to).dividedBy(period);

        // migration stops at the current time, fast forward-past end of migrated values, plus 1 day so all values are aggregated (values are all prior to boundary)
        timer.setStartTime(to.plus(Duration.ofDays(1L)).toInstant().toEpochMilli());

        PointValueGenerator generator = new LinearPointValueGenerator(from.toInstant(), to.toInstant(), period, 0.0D, 1.0D);
        source.savePointValues(generator.apply(point));
        // sanity check
        assertEquals(inputExpectedSamples, source.dateRangeCount(point, null, null));

        migrationPointValueDao.startMigration();
        migrationPointValueDao.migrationFinished().get(30, TimeUnit.SECONDS);

        // raw values should be empty
        try (var stream = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)) {
            Assert.assertEquals(0L, stream.count());
        }

        List<SeriesValueTime<AggregateValue>> aggregates;
        try (var stream = destination.getAggregateDao().query(point, from, to, null, Duration.ofMinutes(15L))) {
            aggregates = stream.collect(Collectors.toList());
        }

        long outputExpectedSamples = Duration.between(from, to).dividedBy(aggregationPeriod);
        assertEquals(outputExpectedSamples, aggregates.size());
        for (int i = 0; i < outputExpectedSamples; i++) {
            var aggregate = aggregates.get(i);
            assertEquals(point.getSeriesId(), aggregate.getSeriesId());
            assertEquals(from.plus(aggregationPeriod.multipliedBy(i)).toInstant().toEpochMilli(), aggregate.getTime());

            NumericAggregate aggregateValue = ((NumericAggregate) aggregate.getValue());
            assertEquals(180, aggregateValue.getCount());
            assertEquals(180.0 * i + 89.5, aggregateValue.getArithmeticMean(), 0.0D);
        }
    }

    /**
     * Tests migration of raw numeric values to 15-minute aggregates, for 3 days, from 2020-01-01 to 2020-01-04, with
     * the boundary set to 1 day and overlap of 1 day.
     * Verifies the number of aggregate samples in the destination database and the average value for each 15-minute period,
     * and verifies the number of raw samples in the destination database.
     */
    @Test
    public void testMigrationBoundary() throws ExecutionException, InterruptedException, TimeoutException {
        Duration aggregationPeriod = Duration.ofMinutes(15);
        TestMigrationConfig config = new TestMigrationConfig();
        // use 1-hour block size for raw values
        config.setBlockSize(Duration.ofHours(1L));
        config.setAggregationPeriod(aggregationPeriod);
        config.setAggregationBoundary(Duration.ofDays(1L));
        config.setAggregationOverlap(Duration.ofDays(1L));
        // use 1-hour block size for aggregation
        config.setAggregationBlockSize(Duration.ofHours(1L));
        useMigrationConfig(config);

        var dataSource = createMockDataSource();
        var point = createMockDataPoint(dataSource, new MockPointLocatorVO());

        ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC);
        ZonedDateTime to = from.plus(Duration.ofDays(3L));
        Duration period = Duration.ofSeconds(5L);
        long inputExpectedSamples = Duration.between(from, to).dividedBy(period);

        // migration stops at the current time, fast forward-past end of migrated values
        timer.setStartTime(to.toInstant().toEpochMilli());

        PointValueGenerator generator = new LinearPointValueGenerator(from.toInstant(), to.toInstant(), period, 0.0D, 1.0D);
        source.savePointValues(generator.apply(point));
        // sanity check
        assertEquals(inputExpectedSamples, source.dateRangeCount(point, null, null));

        migrationPointValueDao.startMigration();
        migrationPointValueDao.migrationFinished().get(9999, TimeUnit.SECONDS);

        // raw values should be empty for first day
        Assert.assertEquals(0L, countDestinationRaw(point, from, from.plus(Duration.ofDays(1L))));
        // raw values should contain 17280 samples for second day (12 samples per minute, 720 samples per hour)
        Assert.assertEquals(17280L, countDestinationRaw(point, from.plus(Duration.ofDays(1L)), from.plus(Duration.ofDays(2L))));
        // raw values should contain 17280 samples for third day (12 samples per minute, 720 samples per hour)
        Assert.assertEquals(17280L, countDestinationRaw(point, from.plus(Duration.ofDays(2L)), to));

        // aggregate values should contain 96 samples for first day (4 samples per hour)
        Assert.assertEquals(96L, countDestinationAggregates(point, from, from.plus(Duration.ofDays(1L))));
        // aggregate values should contain 96 samples for second day (4 samples per hour)
        Assert.assertEquals(96L, countDestinationAggregates(point, from.plus(Duration.ofDays(1L)), from.plus(Duration.ofDays(2L))));
        // aggregate values should be empty for third day
        Assert.assertEquals(0L, countDestinationAggregates(point, from.plus(Duration.ofDays(2L)), to));
    }

    private long countDestinationAggregates(DataPointVO point, ZonedDateTime from, ZonedDateTime to) {
        MockAggregateDao destinationAggregateDao = (MockAggregateDao) destination.getAggregateDao();
        try (var stream = destinationAggregateDao.queryPreAggregated(point, from, to)) {
            return stream.count();
        }
    }

    private long countDestinationRaw(DataPointVO point, ZonedDateTime from, ZonedDateTime to) {
        try (var stream = destination.streamPointValues(point,
                from.toInstant().toEpochMilli(), to.toInstant().toEpochMilli(),
                null, TimeOrder.ASCENDING)) {
            return stream.count();
        }
    }

}
