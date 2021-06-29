/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.exceptionHandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire, Terry Packer
 */
public class MangoUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MangoUncaughtExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if(!exitOnOutOfMemoryError(t, e)) {
            log.error("Uncaught exception in thread " + t.getName(), e);
        }
    }

    public static boolean exitOnOutOfMemoryError(Thread t, Throwable e) {
        if(e instanceof OutOfMemoryError) {
            log.error("Uncaught Out Of Memory exception in thread " + t.getName() + " Mango will now terminate.", e);
            System.exit(1);
            return true;
        }else {
            return false;
        }
    }
}
