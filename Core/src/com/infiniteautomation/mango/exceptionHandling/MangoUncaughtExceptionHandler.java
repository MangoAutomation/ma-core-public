/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.exceptionHandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class MangoUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception in thread " + t.getName(), e);
    }

}
