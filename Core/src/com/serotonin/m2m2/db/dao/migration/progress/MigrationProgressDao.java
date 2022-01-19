/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration.progress;

import java.util.Optional;
import java.util.stream.Stream;

import com.serotonin.db.TransactionCapable;

/**
 * @author Jared Wiltshire
 */
public interface MigrationProgressDao extends TransactionCapable {
    void insert(MigrationProgress progress);

    void bulkInsert(Stream<MigrationProgress> stream);

    void update(MigrationProgress progress);

    void deleteAll();

    Optional<MigrationProgress> get(int seriesId);

    int count();

    Stream<MigrationProgress> stream();
}
