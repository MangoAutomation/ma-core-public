/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;


/**
 * Real time timer that ensures all tasks of the same type are
 * run in order of the time they were submitted via the OrderedThreadPoolExecutor
 *
 * @author Terry Packer
 *
 */
public class OrderedRealTimeTimer extends RealTimeTimer {

    public OrderedRealTimeTimer() {
        super();
    }

    public OrderedRealTimeTimer(ZoneId zone) {
        super(zone);
    }

    public void init(OrderedThreadPoolExecutor executorService, int threadPriority){
        OrderedTimerThread timer = new OrderedTimerThread(queue, executorService, timeSource);
        timer.setName("Ordered RealTime Timer");
        timer.setDaemon(false);
        timer.setPriority(threadPriority);
        super.init(timer);
    }

    @Override
    public void init(ExecutorService executor){
        //Enforce the rule of requiring ordered thread pool
        OrderedThreadPoolExecutor otpExecutor = (OrderedThreadPoolExecutor) executor;
        this.init(otpExecutor, Thread.MAX_PRIORITY);
    }

    @Override
    public void init() {
        this.init(new OrderedThreadPoolExecutor(0, 1000, 30L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), false, timeSource));
    }

    @Override
    public void init(TimerThread timer){
        //Check on cast
        super.init(timer);
    }

    @Override
    public OrderedRealTimeTimer withZone(ZoneId zone) {
        return new OrderedRealTimeTimer(zone);
    }
}
