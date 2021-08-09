/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 *
 * @author Terry Packer
 */
@Service
public class ServerInformationService {

    private static final Logger LOG = LoggerFactory.getLogger(ServerInformationService.class);
    private final Environment env;

    private HardwareAbstractionLayer hal;
    private OperatingSystem os;
    private boolean failedToLoad;

    //Mango Process ID
    private final Object mutex = new Object();
    private OSProcess process;
    private volatile long processUpdateTime = Long.MIN_VALUE;

    //Process CPU Load
    private volatile Double processCpuUsage;
    private volatile long processCpuUsageUpdateTime = Long.MIN_VALUE;
    private long previousProcessTime;

    //System CPU Load
    private volatile Double systemLoad;
    private volatile long systemLoadUpdateTime = Long.MIN_VALUE;
    private long[] previousSystemLoadTicks;

    // minimum period for CPU usage and system load
    private static final long MIN_PERIOD_CPU_AND_LOAD = 1000;

    @Autowired
    public ServerInformationService(Environment env) {
        this.env = env;

        try {
            SystemInfo si = new SystemInfo();
            this.hal = si.getHardware();
            this.os = si.getOperatingSystem();
            this.process = os.getProcess(os.getProcessId());
            this.failedToLoad = false;
        } catch(Throwable e) {
            //If no JNA is supported
            LOG.error("Server Information Service failed to start, no data will be availble on server hardware or processes", e);
            this.hal = null;
            this.os = null;
            this.process = null;
            this.failedToLoad = true;
        }
    }

    /**
     * Get the Operating System
     * @return
     */
    public OperatingSystem getOperatingSystem() {
        return os;
    }

    /**
     * Get the hardware abstraction layer
     * @return
     */
    public HardwareAbstractionLayer getHardware() {
        return hal;
    }

    /**
     * Get Processor
     * @return
     */
    public CentralProcessor getProcessor() {
        return hal.getProcessor();
    }

    /**
     * Get the process running Mango
     * @return
     */
    public OSProcess getMangoProcess() {
        return process;
    }

    /**
     * Updates the processAttributes when the timestamp is newer than last time the attributes were updated.
     *
     * @param ts current time
     * @return the process
     */
    public OSProcess updateProcessAttributes(long ts) {
        if (ts > processUpdateTime) {
            synchronized (mutex) {
                if (ts > processUpdateTime) {
                    if (process != null) {
                        process.updateAttributes();
                    }
                    this.processUpdateTime = ts;
                }
            }
        }
        return process;
    }

    public Double processCpuLoadPercent() {
        return processCpuLoadPercent(Common.timer.currentTimeMillis());
    }

    /**
     * Compute the CPU load of the Mango process,
     *  this limits to only allow an updated value every
     *  MIN_PROCESS_POLL_PERIOD ms to increase accuracy.
     *
     *  This is mildly thread safe in terms of accuracy, as
     *   it is possible to call this faster than desired
     *   but will only result is slightly less accurate readings
     *
     * @return
     */
    public Double processCpuLoadPercent(long timestamp) {
        if (failedToLoad) {
            return null;
        }

        //https://github.com/oshi/oshi/issues/359
        long lastUpdateTime = processCpuUsageUpdateTime;
        if (timestamp > lastUpdateTime + MIN_PERIOD_CPU_AND_LOAD) {
            synchronized (mutex) {
                if (timestamp > lastUpdateTime + MIN_PERIOD_CPU_AND_LOAD) {
                    CentralProcessor processor = getProcessor();
                    int cpuCount = processor.getLogicalProcessorCount();
                    OSProcess process = updateProcessAttributes(timestamp);

                    long processTime = process.getKernelTime() + process.getUserTime();
                    long period = timestamp - processCpuUsageUpdateTime;

                    if (lastUpdateTime > Long.MIN_VALUE && period > 0) {
                        // If we have both a previous and a current time
                        // we can calculate the CPU usage
                        long processTimeDelta = processTime - previousProcessTime;
                        this.processCpuUsage = ((processTimeDelta / ((double) period)) / cpuCount) * 100.0D;
                    }
                    this.previousProcessTime = processTime;
                    this.processCpuUsageUpdateTime = timestamp;
                }
            }
        }

        return this.processCpuUsage;
    }

    /**
     * Get the load average
     *
     * @param increment - 1=1 minute, 2=5 minute, 3=15 minute
     * @return
     */
    public Double systemLoadAverage(int increment) throws ValidationException {
        if (failedToLoad) {
            return null;
        }
        CentralProcessor processor = getProcessor();
        double[] loadAverage = processor.getSystemLoadAverage(increment);
        return loadAverage[increment - 1];
    }

    public Double systemCpuLoadPercent() {
        return systemCpuLoadPercent(Common.timer.currentTimeMillis());
    }

    /**
     * Returns the "recent cpu usage" for the whole system by counting ticks from a previous call
     */
    public Double systemCpuLoadPercent(long timestamp) {
        if (failedToLoad) {
            return null;
        }

        long lastUpdateTime = systemLoadUpdateTime;
        if (timestamp > lastUpdateTime + MIN_PERIOD_CPU_AND_LOAD) {
            synchronized (mutex) {
                if (timestamp > lastUpdateTime + MIN_PERIOD_CPU_AND_LOAD) {
                    CentralProcessor processor = getProcessor();

                    long[] ticks = processor.getSystemCpuLoadTicks();
                    if (previousSystemLoadTicks != null) {
                        this.systemLoad = processor.getSystemCpuLoadBetweenTicks(previousSystemLoadTicks) * 100.0D;
                    }
                    this.previousSystemLoadTicks = ticks;
                    this.systemLoadUpdateTime = timestamp;
                }
            }
        }

        return this.systemLoad;
    }

    /**
     * Get the logical processors of the machine, this will never return null
     *  but could be an empty array.
     * @return
     */
    public List<LogicalProcessor> availableProcessors() {
        if(failedToLoad) {
            return Collections.emptyList();
        }

        CentralProcessor processor = getProcessor();
        List<LogicalProcessor> processors = processor.getLogicalProcessors();
        if(processors == null) {
            return Collections.emptyList();
        }else {
            return processors;
        }
    }
}
