/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.db;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 * {@link org.springframework.transaction.support.TransactionCallbackWithoutResult TransactionCallbackWithoutResult} is not a functional interface. Implement our own.
 * @author Jared Wiltshire
 */
@FunctionalInterface
public interface TransactionCallbackNoResult extends TransactionCallback<Object> {
    @Override
    default Object doInTransaction(TransactionStatus status) {
        doInTransactionNoResult(status);
        return null;
    }

    void doInTransactionNoResult(TransactionStatus status);
}