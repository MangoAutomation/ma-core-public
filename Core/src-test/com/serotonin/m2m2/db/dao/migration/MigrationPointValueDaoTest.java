/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockPointValueDao;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgressDao;
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
        ApplicationContext context = getLifecycle().getRuntimeContext();
        this.migrationPointValueDao = context.getBean(MigrationPointValueDao.class);
        this.migrationConfig = context.getBean(TestMigrationConfig.class);
        this.source = context.getBean("source", MockPointValueDao.class);
        this.destination = context.getBean("destination", MockPointValueDao.class);
    }

    @Test
    public void test() {
        // TODO
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
