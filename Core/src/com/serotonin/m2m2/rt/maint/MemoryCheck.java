/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.maint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * @author Matthew Lohbihler
 */
public class MemoryCheck extends TimerTask {
    private static final Logger log = LoggerFactory.getLogger(MemoryCheck.class);
    private static final long TIMEOUT = 1000 * 5; // Run every five seconds.

    /**
     * This method will set up the memory checking job. It assumes that the corresponding system setting for running
     * this job is true.
     */
    public static void start() {
        Common.backgroundProcessing.schedule(new MemoryCheck());
    }

    public MemoryCheck() {
        super(new FixedRateTrigger(TIMEOUT, TIMEOUT), "Memory check task", "MemCheck", 0);
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
