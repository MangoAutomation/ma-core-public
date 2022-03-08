/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.db;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.PointValueDaoDefinition;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.migration.MigrationConfig;
import com.serotonin.m2m2.db.dao.migration.MigrationPointValueDao;
import com.serotonin.m2m2.db.dao.migration.progress.MigrationProgressDao;
import com.serotonin.m2m2.module.ConditionalDefinition;
import com.serotonin.util.properties.MangoConfigurationWatcher.MangoConfigurationReloadedEvent;

@ConditionalDefinition("db.migration.enabled")
public class MigrationPointValueDaoDefinition extends PointValueDaoDefinition {

    final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired DataPointDao dataPointDao;
    @Autowired Environment env;
    @Autowired ExecutorService executorService;
    @Autowired ScheduledExecutorService scheduledExecutorService;
    @Autowired ConfigurableApplicationContext context;
    @Autowired Clock clock;
    @Autowired List<PointValueDaoDefinition> definitions;
    @Autowired MigrationProgressDao migrationProgressDao;
    @Autowired MigrationConfig config;

    PointValueDaoDefinition primary;
    PointValueDaoDefinition secondary;
    MigrationPointValueDao pointValueDao;

    @Override
    public void initialize() {
        if (definitions.size() < 2) {
            throw new IllegalStateException("Migration requires two enabled time-series databases");
        }

        // list of injected definitions does not include ourselves, get the next two definitions
        this.primary = definitions.get(0);
        this.secondary = definitions.get(1);
        primary.initialize();
        secondary.initialize();

        this.pointValueDao = new MigrationPointValueDao(
                primary.getPointValueDao(),
                secondary.getPointValueDao(),
                dataPointDao,
                executorService,
                scheduledExecutorService,
                clock,
                migrationProgressDao,
                config);

        context.addApplicationListener((ApplicationListener<MangoConfigurationReloadedEvent>) e -> pointValueDao.reloadConfig());

        if (log.isInfoEnabled()) {
            log.info("Time series migration enabled, from {} (secondary) to {} (primary)",
                    secondary.getClass().getSimpleName(),
                    primary.getClass().getSimpleName());
        }
    }

    @Override
    public void shutdown() {
        // our own pointValueDao is closed by Spring (it implements AutoCloseable)

        if (primary != null) {
            try {
                primary.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down primary point value DAO", e);
            }
        }
        if (secondary != null) {
            try {
                secondary.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down secondary point value DAO", e);
            }
        }
    }

    @Override
    public PointValueDao getPointValueDao() {
        return pointValueDao;
    }

    @Override
    public int getOrder() {
        return Common.envProps.getInt("db.migration.order", Integer.MAX_VALUE);
    }
}
