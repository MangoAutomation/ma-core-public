/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.exceptionHandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.TerminationReason;

/**
 * @author Jared Wiltshire, Terry Packer
 */
public class MangoUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MangoUncaughtExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if(e instanceof OutOfMemoryError) {
            log.error("Uncaught Out Of Memory exception in thread " + t.getName() + " Mango will now terminate.", e);
            System.exit(TerminationReason.OUT_OF_MEMORY_ERROR.getExitStatus());
        }else {
            log.error("Uncaught exception in thread " + t.getName(), e);
        }
    }
}
