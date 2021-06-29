/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

/**
 * @author Jared Wiltshire
 */
public class ServerStatus {
    public static final ServerStatus NOT_RUNNING = new ServerStatus(0, 0, 0);

    private final int threads;
    private final int idleThreads;
    private final int queueSize;

    public ServerStatus(int threads, int idleThreads, int queueSize) {
        super();
        this.threads = threads;
        this.idleThreads = idleThreads;
        this.queueSize = queueSize;
    }

    public int getThreads() {
        return threads;
    }
    public int getIdleThreads() {
        return idleThreads;
    }
    public int getQueueSize() {
        return queueSize;
    }
}
