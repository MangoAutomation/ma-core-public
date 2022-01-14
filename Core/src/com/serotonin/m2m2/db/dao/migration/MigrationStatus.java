/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

/**
 * @author Jared Wiltshire
 */
public enum MigrationStatus {
    NOT_STARTED,
    RUNNING,
    MIGRATED,
    SKIPPED,
    ERROR
}
