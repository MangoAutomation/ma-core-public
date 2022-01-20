/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockPointValueDao;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao.TimeOrder;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgressDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.timer.AbstractTimer;

/**
 * @author Jared Wiltshire
 */
public class MigrationPointValueDaoTest extends MangoTestBase {

    MigrationPointValueDao migrationPointValueDao;
    TestMigrationConfig migrationConfig;
    MockPointValueDao source;
    MockPointValueDao destination;

    @Override
    public void before() {
        super.before();

        ApplicationContext context = MangoTestBase.lifecycle.getRuntimeContext();
        this.migrationPointValueDao = context.getBean(MigrationPointValueDao.class);
        this.migrationConfig = context.getBean(TestMigrationConfig.class);
        this.source = context.getBean("source", MockPointValueDao.class);
        this.destination = context.getBean("destination", MockPointValueDao.class);
    }

    @Test
    public void singleValue() throws ExecutionException, InterruptedException, TimeoutException {
        var dataSource = createMockDataSource();
        var point = createMockDataPoint(dataSource, new MockPointLocatorVO());

        var sourceValues = List.of(new PointValueTime(0.0D, 0L));
        var batchInsertValues = sourceValues.stream().map(v -> new BatchPointValueImpl(point, v))
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
            assertEquals(sourceValue.getValue(), destinationValue.getValue());
        }
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.addRuntimeContextConfiguration(MigrationSpringConfig.class);
        return lifecycle;
    }

    private static class MigrationSpringConfig {

        @Bean
        @Primary
        public MigrationConfig migrationConfig() {
            return new TestMigrationConfig();
        }

        @Bean("source")
        public PointValueDao source() {
            return new MockPointValueDao();
        }

        @Bean("destination")
        public PointValueDao destination() {
            return new MockPointValueDao();
        }

        @Bean
        @Primary
        public PointValueDao pointValueDao(@Qualifier("source") PointValueDao source,
                                           @Qualifier("destination") PointValueDao destination,
                                           DataPointDao dataPointDao,
                                           AbstractTimer timer,
                                           MigrationProgressDao migrationProgressDao,
                                           MigrationConfig config,
                                           ExecutorService executor,
                                           ScheduledExecutorService scheduledExecutor) {

            return new MigrationPointValueDao(destination, source, dataPointDao,
                    executor, scheduledExecutor, timer, migrationProgressDao, config);
        }
    }
}
