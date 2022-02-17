/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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
    public Duration getMigrationPeriod() {
        long period = env.getProperty("db.migration.period", Long.class, 1L);
        ChronoUnit periodUnit = env.getProperty("db.migration.periodUnit", ChronoUnit.class, ChronoUnit.DAYS);
        return Duration.of(period, periodUnit);
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
        return env.getProperty("db.migration.readChunkSize", int.class, 10000);
    }

    @Override
    public int getWriteChunkSize() {
        return env.getProperty("db.migration.writeChunkSize", int.class, 10000);
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
        return vo -> true;
    }

    @Override
    public TemporalAmount getAggregationPeriod() {
        long amount = env.getProperty("db.migration.aggregationPeriod", Long.class, 0L);
        ChronoUnit unit = env.getProperty("db.migration.aggregationPeriodUnit", ChronoUnit.class, ChronoUnit.MINUTES);

        if (amount <= 0) {
            return null;
        }

        if (unit.isTimeBased()) {
            return Duration.of(amount, unit);
        }

        switch (unit) {
            case DAYS: return Period.ofDays(Math.toIntExact(amount));
            case WEEKS: return Period.ofWeeks(Math.toIntExact(amount));
            case MONTHS: return Period.ofMonths(Math.toIntExact(amount));
            case YEARS: return Period.ofYears(Math.toIntExact(amount));
        }

        throw new IllegalStateException("Unsupported unit: " + unit);
    }
}
