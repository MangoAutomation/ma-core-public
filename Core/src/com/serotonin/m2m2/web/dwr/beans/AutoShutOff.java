/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import com.serotonin.m2m2.Common;

/**
 * Checks every 10 seconds to see if it's last query time is older than one minute. If so, the shutOff method is called.
 * 
 * @author Matthew Lohbihler
 */
abstract public class AutoShutOff extends Thread {
    public static final long DEFAULT_TIMEOUT = 60000;

    private long lastQuery;
    private volatile boolean running;
    private final long timeout;

    public AutoShutOff() {
        this(DEFAULT_TIMEOUT);
    }

    public AutoShutOff(long timeout) {
        this.timeout = timeout;
        update();
        start();
    }

    public void update() {
        lastQuery = Common.timer.currentTimeMillis();
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            if (Common.timer.currentTimeMillis() - lastQuery > timeout) {
                running = false;
                shutOff();
                break;
            }

            synchronized (this) {
                try {
                    wait(10000);
                }
                catch (InterruptedException e) {
                    // no op
                }
            }
        }
    }

    public void cancel() {
        running = false;
        synchronized (this) {
            notify();
        }
    }

    abstract protected void shutOff();
}
