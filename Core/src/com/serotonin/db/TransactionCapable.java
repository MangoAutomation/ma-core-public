/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.db;

import java.util.function.Consumer;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Jared Wiltshire
 */
public interface TransactionCapable {
    PlatformTransactionManager getTransactionManager();

    default <T> T doInTransaction(TransactionCallback<T> callback) {
        return this.getTransactionTemplate().execute(callback);
    }

    default void doInTransaction(Consumer<TransactionStatus> callback) {
        this.getTransactionTemplate().executeWithoutResult(callback);
    }

    default TransactionTemplate getTransactionTemplate() {
        return new TransactionTemplate(getTransactionManager());
    }
}
