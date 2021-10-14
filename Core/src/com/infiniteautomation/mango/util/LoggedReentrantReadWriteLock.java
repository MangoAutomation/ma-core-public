/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggedReentrantReadWriteLock extends ReentrantReadWriteLock {
    private final Logger log = LoggerFactory.getLogger(LoggedReentrantReadWriteLock.class);

    @Override
    public ReadLock readLock() {
        log.info(Thread.currentThread().getName() + " getting read lock.");
        return super.readLock();
    }

    @Override
    public WriteLock writeLock() {
        log.info(Thread.currentThread().getName() + " getting write lock.");
        return super.writeLock();
    }
}
