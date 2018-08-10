/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Creates executors and shuts them down
 * @author Jared Wiltshire
 */
@Component
public class MangoExecutors {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "Mango scheduled executor");
            thread.setDaemon(true);
            return thread;
        }
    });

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "Mango executor");
            thread.setDaemon(true);
            return thread;
        }
    });

    @PreDestroy
    public void destroy() {
        scheduledExecutor.shutdown();
        executor.shutdown();

        try {
            scheduledExecutor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to shutdown scheduled executor service cleanly");
            }

            scheduledExecutor.shutdownNow();
        }

        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to shutdown executor service cleanly");
            }

            executor.shutdownNow();
        }
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

}
