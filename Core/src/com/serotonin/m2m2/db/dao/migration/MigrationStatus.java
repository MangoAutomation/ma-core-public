/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

/**
 * @author Jared Wiltshire
 */
public enum MigrationStatus {
    NOT_STARTED(false),
    INITIAL_PASS_COMPLETE(false),
    RUNNING(false),
    NO_DATA(true),
    MIGRATED(true),
    SKIPPED(true),
    ERROR(true);

    private final boolean complete;

    MigrationStatus(boolean complete) {
        this.complete = complete;
    }

    /**
     * @return true if no more work to do
     */
    public boolean isComplete() {
        return complete;
    }
}
