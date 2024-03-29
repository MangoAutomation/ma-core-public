/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
@Component
public class DefaultMigrationConfig implements MigrationConfig {

    private final Environment env;

    @Autowired
    public DefaultMigrationConfig(Environment env) {
        this.env = env;
    }

    @Override
    public Instant getMigrateFromTime() {
        String fromStr = env.getProperty("db.migration.fromDate");
        return fromStr == null ? null : ZonedDateTime.parse(fromStr).toInstant();
    }

    @Override
    public Duration getBlockSize() {
        return env.getProperty("db.migration.blockSize", Duration.class, Duration.ofDays(1L));
    }

    @Override
    public Duration getAggregationBlockSize() {
        return env.getProperty("db.migration.aggregation.blockSize", Duration.class, Duration.ofDays(14L));
    }

    @Override
    public int getMaxAttempts() {
        return env.getProperty("db.migration.maxAttempts", int.class, 5);
    }

    @Override
    public boolean isAutoStart() {
        return true;
    }

    @Override
    public boolean isStartNewMigration() {
        return env.getProperty("db.migration.startNewMigration", boolean.class, false);
    }

    @Override
    public int getLogPeriodSeconds() {
        return env.getProperty("db.migration.logPeriodSeconds", int.class, 60);
    }

    @Override
    public int getReadChunkSize() {
        return env.getProperty("db.migration.readChunkSize", int.class, 16_384);
    }

    @Override
    public int getWriteChunkSize() {
        return env.getProperty("db.migration.writeChunkSize", int.class, 16_384);
    }

    @Override
    public int getThreadCount() {
        return env.getProperty("db.migration.threadCount", int.class, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
    }

    @Override
    public Duration getCloseWait() {
        long closeWait = env.getProperty("db.migration.closeWait", Long.class, 1L);
        ChronoUnit closeWaitUnit = env.getProperty("db.migration.closeWaitUnit", ChronoUnit.class, ChronoUnit.MINUTES);
        return Duration.of(closeWait, closeWaitUnit);
    }

    @Override
    public Predicate<DataPointVO> getDataPointFilter() {
        var dataTypesArray = env.getProperty("db.migration.filter.dataTypes", DataType[].class);
        if (dataTypesArray == null) {
            return vo -> true;
        }

        var dataTypes = EnumSet.noneOf(DataType.class);
        dataTypes.addAll(Arrays.asList(dataTypesArray));
        return vo -> dataTypes.contains(vo.getPointLocator().getDataType());
    }

    @Override
    public ZoneId getZone() {
        return env.getProperty("db.migration.aggregation.zone", ZoneId.class, ZoneId.systemDefault());
    }

    @Override
    public Set<DataType> getAggregationDataTypes() {
        var dataTypes = env.getProperty("db.migration.aggregation.dataTypes", DataType[].class, new DataType[] {DataType.NUMERIC});
        var set = EnumSet.noneOf(DataType.class);
        set.addAll(Arrays.asList(dataTypes));
        return set;
    }

    @Override
    public TemporalAmount getAggregationPeriod() {
        return env.getProperty("db.migration.aggregation.period", TemporalAmount.class);
    }

    @Override
    public TemporalAmount getAggregationBoundary() {
        return env.getProperty("db.migration.aggregation.boundary", TemporalAmount.class, Duration.ZERO);
    }

    @Override
    public TemporalAmount getAggregationOverlap() {
        return env.getProperty("db.migration.aggregation.overlap", TemporalAmount.class, Duration.ZERO);
    }
}
