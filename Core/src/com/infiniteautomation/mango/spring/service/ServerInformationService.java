/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;

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

    private static final Log LOG = LogFactory.getLog(ServerInformationService.class);

    private HardwareAbstractionLayer hal;
    private OperatingSystem os;
    private boolean failedToLoad;


    //Mango Process ID
    private int pid;

    //Process CPU Load
    private long previousProcessTime = -1;
    private long lastProcessCpuPollTime;
    private Double lastProcessLoad;
    private static final long MIN_PROCESS_LOAD_POLL_PERIOD = 1000;

    //System CPU Load
    private long lastTicks[];
    private long lastSystemCpuPollTime;
    private Double lastSystemCpuLoad;
    private static final long MIN_CPU_LOAD_POLL_PERIOD = 1000;

    public ServerInformationService() {
        try {
            SystemInfo si = new SystemInfo();
            this.hal = si.getHardware();
            this.os = si.getOperatingSystem();
            this.pid = os.getProcessId();
            this.failedToLoad = false;
        }catch(Throwable e) {
            //If no JNA is supported
            LOG.fatal("Server Information Service failed to start, no data will be availble on server hardware or processes", e);
            this.hal = null;
            this.os = null;
            this.pid = -1;
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
        return os.getProcess(this.pid);
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
    public Double processCpuLoadPercent() {
        if(failedToLoad) {
            return null;
        }
        //https://github.com/oshi/oshi/issues/359
        long lpcpt = this.lastProcessCpuPollTime;
        long now = Common.timer.currentTimeMillis();
        if(now < lpcpt + MIN_PROCESS_LOAD_POLL_PERIOD) {
            return this.lastProcessLoad;
        }

        CentralProcessor processor = getProcessor();
        int cpuCount = processor.getLogicalProcessorCount();
        OSProcess p = getMangoProcess();

        if(this.lastProcessCpuPollTime == 0) {
            this.lastProcessCpuPollTime = now;
            return null;
        }

        long currentTime = p.getKernelTime() + p.getUserTime();
        long processCpuPollPeriod = now - this.lastProcessCpuPollTime;
        this.lastProcessCpuPollTime = now;

        if(previousProcessTime > 0 && processCpuPollPeriod > 0) {
            // If we have both a previous and a current time
            // we can calculate the CPU usage
            long timeDifference = currentTime - previousProcessTime;
            this.lastProcessLoad = ((timeDifference / ((double) processCpuPollPeriod)) / cpuCount)*100d;
        }
        previousProcessTime = currentTime;
        return this.lastProcessLoad;
    }

    /**
     * Get the load average
     * @param increment - 1=1 minute, 2=5 minute, 3=15 minute
     * @return
     */
    public Double systemLoadAverage(int increment) throws ValidationException {
        if(failedToLoad) {
            return null;
        }

        if(increment < 1 || increment > 3) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("increment", "validate.invalidValue");
        }

        CentralProcessor processor = getProcessor();
        double[] loadAverage = processor.getSystemLoadAverage(increment);

        return loadAverage[increment - 1];
    }

    /**
     * Returns the "recent cpu usage" for the whole system by counting ticks from a previous call
     *
     *  This is limited to only allow an updated value every
     *  MIN_CPU_LOAD_POLL_PERIOD ms to increase accuracy.
     *
     *  This is mildly thread safe in terms of accuracy, as
     *   it is possible to call this faster than desired
     *   but will only result is slightly less accurate readings
     */
    public Double systemCpuLoadPercent() {
        if(failedToLoad) {
            return null;
        }

        long lpcpt = this.lastSystemCpuPollTime;
        long now = Common.timer.currentTimeMillis();
        if(now < lpcpt + MIN_CPU_LOAD_POLL_PERIOD) {
            return this.lastSystemCpuLoad;
        }

        CentralProcessor processor = getProcessor();

        if(this.lastSystemCpuPollTime == 0) {
            this.lastSystemCpuPollTime = now;
            return null;
        }

        if(this.lastTicks == null) {
            this.lastTicks = processor.getSystemCpuLoadTicks();
            this.lastSystemCpuPollTime = now;
            return null;
        }else {
            double load = processor.getSystemCpuLoadBetweenTicks(this.lastTicks)*100d;
            this.lastTicks = processor.getSystemCpuLoadTicks();
            this.lastSystemCpuPollTime = now;
            return load;
        }
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
