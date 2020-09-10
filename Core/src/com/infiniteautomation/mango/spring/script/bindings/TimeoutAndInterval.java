/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.bindings;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.script.Bindings;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.permissions.TimeoutAndIntervalPermission;
import com.serotonin.m2m2.module.ScriptBindingsDefinition;
import com.serotonin.m2m2.module.ScriptEngineDefinition;

/**
 * @author Jared Wiltshire
 */
public class TimeoutAndInterval extends ScriptBindingsDefinition {

    @Autowired
    ScheduledExecutorService scheduledExecutorService;
    @Autowired
    TimeoutAndIntervalPermission timeoutAndIntervalPermission;

    @Override
    public void addBindings(MangoScript script, Bindings engineBindings, Object synchronizationObject,
                            ScriptEngineDefinition scriptEngineDefinition) {
        Timers timers = scriptEngineDefinition.singleThreadedAccess() ? new SynchronizedTimers(synchronizationObject) : new Timers();
        engineBindings.put("setTimeout", (SetTimeout) timers::setTimeout);
        engineBindings.put("setInterval", (SetTimeout) timers::setInterval);
        engineBindings.put("clearTimeout", (ClearTimeout) timers::clearTimeout);
        engineBindings.put("clearInterval", (ClearTimeout) timers::clearInterval);
    }

    @Override
    public MangoPermission requiredPermission() {
        return timeoutAndIntervalPermission.getPermission();
    }

    @FunctionalInterface
    public interface SetTimeout {
        TimeoutResult setTimeout(Runnable task, long delay);
    }

    @FunctionalInterface
    public interface ClearTimeout {
        void clearInterval(TimeoutResult intervalID);
    }

    public static class TimeoutResult {
        final ScheduledFuture<?> future;
        private TimeoutResult(ScheduledFuture<?> future) {
            this.future = future;
        }
    }

    class Timers {
        public TimeoutResult setTimeout(Runnable task, long delay) {
            ScheduledFuture<?> future = scheduledExecutorService.schedule(task, delay, TimeUnit.MILLISECONDS);
            return new TimeoutResult(future);
        }

        public TimeoutResult setInterval(Runnable task, long delay) {
            ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(task, delay, delay, TimeUnit.MILLISECONDS);
            return new TimeoutResult(future);
        }

        public void clearTimeout(TimeoutResult timeoutID) {
            if (timeoutID != null) {
                timeoutID.future.cancel(false);
            }
        }

        public void clearInterval(TimeoutResult intervalID) {
            if (intervalID != null) {
                intervalID.future.cancel(false);
            }
        }
    }

    class SynchronizedTimers extends Timers {
        final Object synchronizationObject;

        SynchronizedTimers(Object synchronizationObject) {
            this.synchronizationObject = synchronizationObject;
        }

        @Override
        public TimeoutResult setTimeout(Runnable task, long delay) {
            return super.setTimeout(() -> {
                synchronized (synchronizationObject) {
                    task.run();
                }
            }, delay);
        }

        @Override
        public TimeoutResult setInterval(Runnable task, long delay) {
            return super.setInterval(() -> {
                synchronized (synchronizationObject) {
                    task.run();
                }
            }, delay);
        }
    }
}
