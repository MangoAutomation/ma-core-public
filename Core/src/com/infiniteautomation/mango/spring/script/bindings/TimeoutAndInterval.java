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
        ScheduledFuture<?> setTimeout(Runnable task, long delay);
    }

    @FunctionalInterface
    public interface ClearTimeout {
        void clearInterval(ScheduledFuture<?> intervalID);
    }

    class Timers {
        public ScheduledFuture<?> setTimeout(Runnable task, long delay) {
            return scheduledExecutorService.schedule(task, delay, TimeUnit.MILLISECONDS);
        }

        public ScheduledFuture<?> setInterval(Runnable task, long delay) {
            return scheduledExecutorService.scheduleAtFixedRate(task, delay, delay, TimeUnit.MILLISECONDS);
        }

        public void clearTimeout(ScheduledFuture<?> timeoutID) {
            if (timeoutID != null) {
                timeoutID.cancel(false);
            }
        }

        public void clearInterval(ScheduledFuture<?> intervalID) {
            if (intervalID != null) {
                intervalID.cancel(false);
            }
        }
    }

    class SynchronizedTimers extends Timers {
        final Object synchronizationObject;

        SynchronizedTimers(Object synchronizationObject) {
            this.synchronizationObject = synchronizationObject;
        }

        @Override
        public ScheduledFuture<?> setTimeout(Runnable task, long delay) {
            return super.setTimeout(() -> {
                synchronized (synchronizationObject) {
                    task.run();
                }
            }, delay);
        }

        @Override
        public ScheduledFuture<?> setInterval(Runnable task, long delay) {
            return super.setInterval(() -> {
                synchronized (synchronizationObject) {
                    task.run();
                }
            }, delay);
        }
    }
}
