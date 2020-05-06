/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
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
    public <X> X doInTransaction(TransactionCallback<X> callback);
    public void doInTransaction(Consumer<TransactionStatus> callback);
    public PlatformTransactionManager getTransactionManager();

    public default TransactionTemplate getTransactionTemplate() {
        return new TransactionTemplate(getTransactionManager());
    }
}
