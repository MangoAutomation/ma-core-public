/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.monitor.IntegerMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.ServerStatus;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
@Service
public class ServerMonitoringService implements ValueMonitorOwner {

    public static final String SERVER_THREADS = "internal.monitor.SERVER_THREADS";
    public static final String SERVER_IDLE_THREADS = "internal.monitor.SERVER_IDLE_THREADS";
    public static final String SERVER_QUEUE_SIZE = "internal.monitor.SERVER_QUEUE_SIZE";

    private final ScheduledExecutorService scheduledExecutor;
    private final long period;
    private final IntegerMonitor threads = new IntegerMonitor(SERVER_THREADS, new TranslatableMessage(SERVER_THREADS), this);
    private final IntegerMonitor idleThreads = new IntegerMonitor(SERVER_IDLE_THREADS, new TranslatableMessage(SERVER_IDLE_THREADS), this);
    private final IntegerMonitor queueSize = new IntegerMonitor(SERVER_QUEUE_SIZE, new TranslatableMessage(SERVER_QUEUE_SIZE), this);
    private volatile ScheduledFuture<?> scheduledFuture;
    private final IMangoLifecycle lifecycle;

    @Autowired
    private ServerMonitoringService(ScheduledExecutorService scheduledExecutor, @Value("${internal.monitor.pollPeriod:10000}") long period, IMangoLifecycle lifecycle) {
        this.scheduledExecutor = scheduledExecutor;
        this.period = period;

        Common.MONITORED_VALUES.addIfMissingStatMonitor(threads);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(idleThreads);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(queueSize);

        this.lifecycle = lifecycle;
    }

    @PostConstruct
    private void postConstruct() {
        this.scheduledFuture = scheduledExecutor.scheduleAtFixedRate(this::doPoll, 0, this.period, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    private void preDestroy() {
        ScheduledFuture<?> scheduledFuture = this.scheduledFuture;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    private void doPoll() {
        ServerStatus status = lifecycle.getServerStatus();
        threads.setValue(status.getThreads());
        idleThreads.setValue(status.getIdleThreads());
        queueSize.setValue(status.getQueueSize());
    }

    @Override
    public void reset(String monitorId) {
        // nop
    }
}
