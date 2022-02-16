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

import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.infiniteautomation.mango.pointvalue.generator.BrownianPointValueGenerator;
import com.infiniteautomation.mango.pointvalue.generator.LinearPointValueGenerator;
import com.infiniteautomation.mango.pointvalue.generator.PointValueGenerator;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockPointValueDao;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgressDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
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
    public void before() {
        super.before();

        ApplicationContext context = MangoTestBase.lifecycle.getRuntimeContext();
        this.source = new MockPointValueDao();
        this.destination = new MockPointValueDao();
        this.timer = context.getBean(SimulationTimer.class);
        useMigrationConfig(new TestMigrationConfig());
    }

    @Override
    public void after() {
        super.after();
        this.migrationPointValueDao.reset();

        // ensure point data is deleted, MangoTestBase calls this also, but it may not be called for both source and destination
        this.source.deleteAllPointData();
        this.destination.deleteAllPointData();
    }

    private void useMigrationConfig(TestMigrationConfig config) {
        this.migrationConfig = config;

        ApplicationContext context = MangoTestBase.lifecycle.getRuntimeContext();
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

        var destinationValues = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)
                .collect(Collectors.toList());

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

        var destinationValues = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)
                .collect(Collectors.toList());

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
            var destinationValues = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)
                    .collect(Collectors.toList());

            assertEquals(expectedSamples, destinationValues.size());
            for (int i = 0; i < expectedSamples; i++) {
                var destinationValue = destinationValues.get(i);
                assertEquals(point.getSeriesId(), destinationValue.getSeriesId());
                assertEquals(from.plus(period.multipliedBy(i)).toInstant().toEpochMilli(), destinationValue.getTime());
            }
        }
    }

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

        // migration stops at the current time
        timer.setStartTime(to.toInstant().toEpochMilli());

        PointValueGenerator generator = new LinearPointValueGenerator(from.toInstant(), to.toInstant(), period, 0.0D, 1.0D);
        source.savePointValues(generator.apply(point));
        // sanity check
        assertEquals(inputExpectedSamples, source.dateRangeCount(point, null, null));

        migrationPointValueDao.startMigration();
        migrationPointValueDao.migrationFinished().get(30, TimeUnit.SECONDS);

        var destinationValues = destination.streamPointValues(point, null, null, null, TimeOrder.ASCENDING)
                .collect(Collectors.toList());

        long outputExpectedSamples = Duration.between(from, to).dividedBy(aggregationPeriod);
        assertEquals(outputExpectedSamples, destinationValues.size());
        for (int i = 0; i < outputExpectedSamples; i++) {
            var destinationValue = destinationValues.get(i);
            assertEquals(point.getSeriesId(), destinationValue.getSeriesId());
            assertEquals(from.plus(aggregationPeriod.multipliedBy(i)).toInstant().toEpochMilli(), destinationValue.getTime());

            double expected = 180.0 * i + 89.5;
            assertEquals(expected, destinationValue.getDoubleValue(), 0.0D);
        }
    }
}
