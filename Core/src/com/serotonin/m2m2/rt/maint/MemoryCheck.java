/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * @author Matthew Lohbihler
 */
public class MemoryCheck extends TimerTask {
    private static final Log log = LogFactory.getLog(MemoryCheck.class);
    private static final long TIMEOUT = 1000 * 5; // Run every five seconds.

    /**
     * This method will set up the memory checking job. It assumes that the corresponding system setting for running
     * this job is true.
     */
    public static void start() {
        Common.timer.schedule(new MemoryCheck());
    }

    public MemoryCheck() {
        super(new FixedRateTrigger(TIMEOUT, TIMEOUT));
    }

    @Override
    public void run(long fireTime) {
        memoryCheck();
    }

    public static void memoryCheck() {
        Runtime rt = Runtime.getRuntime();
        log.info("Free=" + rt.freeMemory() + ", total=" + rt.totalMemory() + ", max=" + rt.maxMemory());
    }
}
