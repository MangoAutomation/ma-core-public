/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.serotonin.m2m2.Common;
import com.serotonin.util.ILifecycle;

abstract public class SendThread extends Thread implements ILifecycle {
    private static final Log LOG = LogFactory.getLog(SendThread.class);
    private boolean running;
    private final RunAs runAs;

    public SendThread(String threadName) {
        super(threadName);
        this.runAs = Common.getBean(RunAs.class);
    }

    @Override
    public void initialize(boolean safe) {
        running = true;
        start();
    }

    @Override
    public void terminate() {
        running = false;

        // Notify to break out of any wait.
        synchronized (this) {
            notify();
        }

        // Interrupt to break out of any sleep.
        interrupt();
    }

    protected boolean isRunning() {
        return running;
    }

    @Override
    public void joinTermination() {
        try {
            join();
        }
        catch (InterruptedException e) {
            // no op
        }
    }

    @Override
    public void run() {
        try {
            runAs.runAs(runAs.systemSuperadmin(), this::runImpl);
        }
        catch (Exception e) {
            LOG.error("Send thread " + getName() + " failed with an exception", e);
        }
    }

    protected void waitImpl(long time) {
        synchronized (this) {
            try {
                wait(time);
            }
            catch (InterruptedException e1) {
                // no op
            }
        }
    }

    protected void sleepImpl(long time) {
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException e) {
            // no op
        }
    }

    abstract protected void runImpl();
}
