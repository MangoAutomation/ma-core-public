/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
public class TestMigrationConfig implements MigrationConfig {
    Instant migrateFromTime = null;
    Duration migrationPeriod = Duration.ofDays(1L);
    ZoneId zone = ZoneOffset.UTC;
    int maxAttempts = 3;
    boolean autoStart = false;
    boolean startNewMigration = false;
    int logPeriodSeconds = 0;
    int readChunkSize = 10000;
    int writeChunkSize = 10000;
    int threadCount = 1;
    Duration closeWait = Duration.ofSeconds(30L);
    Predicate<DataPointVO> dataPointFilter = p -> true;
    TemporalAmount aggregationPeriod;
    TemporalAmount aggregationEnd = Duration.ZERO;
    TemporalAmount aggregationOverlap = Duration.ZERO;
    Set<DataType> aggregationDataTypes = EnumSet.of(DataType.NUMERIC);

    @Override
    public Instant getMigrateFromTime() {
        return migrateFromTime;
    }

    public void setMigrateFromTime(Instant migrateFromTime) {
        this.migrateFromTime = migrateFromTime;
    }

    @Override
    public Duration getMigrationPeriod() {
        return migrationPeriod;
    }

    public void setMigrationPeriod(Duration migrationPeriod) {
        this.migrationPeriod = migrationPeriod;
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    @Override
    public boolean isStartNewMigration() {
        return startNewMigration;
    }

    public void setStartNewMigration(boolean startNewMigration) {
        this.startNewMigration = startNewMigration;
    }

    @Override
    public int getLogPeriodSeconds() {
        return logPeriodSeconds;
    }

    public void setLogPeriodSeconds(int logPeriodSeconds) {
        this.logPeriodSeconds = logPeriodSeconds;
    }

    @Override
    public int getReadChunkSize() {
        return readChunkSize;
    }

    public void setReadChunkSize(int readChunkSize) {
        this.readChunkSize = readChunkSize;
    }

    @Override
    public int getWriteChunkSize() {
        return writeChunkSize;
    }

    public void setWriteChunkSize(int writeChunkSize) {
        this.writeChunkSize = writeChunkSize;
    }

    @Override
    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public Duration getCloseWait() {
        return closeWait;
    }

    public void setCloseWait(Duration closeWait) {
        this.closeWait = closeWait;
    }

    @Override
    public Predicate<DataPointVO> getDataPointFilter() {
        return dataPointFilter;
    }

    @Override
    public TemporalAmount getAggregationPeriod() {
        return aggregationPeriod;
    }

    @Override
    public TemporalAmount getAggregationBoundary() {
        return aggregationEnd;
    }

    public void setDataPointFilter(Predicate<DataPointVO> dataPointFilter) {
        this.dataPointFilter = dataPointFilter;
    }

    public void setAggregationPeriod(TemporalAmount aggregationPeriod) {
        this.aggregationPeriod = aggregationPeriod;
    }

    public void setAggregationEnd(TemporalAmount aggregationEnd) {
        this.aggregationEnd = aggregationEnd;
    }

    @Override
    public ZoneId getZone() {
        return aggregationPeriod != null ? zone : ZoneId.systemDefault();
    }

    public void setZone(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public Set<DataType> getAggregationDataTypes() {
        return aggregationDataTypes;
    }

    public void setAggregationDataTypes(Set<DataType> aggregationDataTypes) {
        this.aggregationDataTypes = aggregationDataTypes;
    }

    @Override
    public TemporalAmount getAggregationOverlap() {
        return aggregationOverlap;
    }

    public void setAggregationOverlap(TemporalAmount aggregationOverlap) {
        this.aggregationOverlap = aggregationOverlap;
    }
}
