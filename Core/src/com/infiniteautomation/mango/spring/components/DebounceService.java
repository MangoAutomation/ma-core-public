/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Used to debounce the execution of a {@link Runnable}. Prevents duplicate actions as a result of events occurring
 * in rapid succession.
 */
@Component
public class DebounceService {

    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService executorService;

    @Autowired
    public DebounceService(ScheduledExecutorService scheduledExecutorService, ExecutorService executorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.executorService = executorService;
    }

    public Debounce debounce(Runnable delegate, int delay, TimeUnit unit) {
        return new Debounce(delegate, delay, unit);
    }

    public class Debounce implements Runnable {
        private final Runnable delegate;
        private final int delay;
        private final TimeUnit unit;
        private final AtomicReference <ScheduledFuture<?>> future = new AtomicReference<>();

        private Debounce(Runnable delegate, int delay, TimeUnit unit) {
            this.delegate = delegate;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public void run() {
            ScheduledFuture<?> newFuture = scheduledExecutorService.schedule(() -> executorService.execute(delegate), delay, unit);
            ScheduledFuture<?> oldFuture = future.getAndSet(newFuture);
            if (oldFuture != null) {
                oldFuture.cancel(false);
            }
        }
    }
}
