/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration.progress;

import com.serotonin.m2m2.db.dao.migration.MigrationStatus;

/**
 * @author Jared Wiltshire
 */
public class MigrationProgress {
    final int seriesId;
    final MigrationStatus status;
    final long timestamp;

    public MigrationProgress(int seriesId, MigrationStatus status, long timestamp) {
        this.seriesId = seriesId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public int getSeriesId() {
        return seriesId;
    }

    public MigrationStatus getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
