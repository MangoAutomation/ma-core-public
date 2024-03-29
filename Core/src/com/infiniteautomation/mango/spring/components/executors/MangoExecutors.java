/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components.executors;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.exceptionHandling.MangoUncaughtExceptionHandler;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;

/**
 * Creates executors and shuts them down
 * @author Jared Wiltshire
 */
@Component
public final class MangoExecutors {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ScheduledExecutorService scheduledExecutor;
    private final ScheduledExecutorService delegatingScheduledExecutor;
    private final ScheduledExecutorService superadminScheduledExecutor;

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "Mango shared executor service");
            thread.setDaemon(true);
            thread.setContextClassLoader(Common.getModuleClassLoader());
            return thread;
        }
    });
    private final ExecutorService delegatingExecutor;
    private final ExecutorService superadminExecutor;

    private volatile Thread futureConverterThread;
    private final LazyInitSupplier<FutureConverter> futureConverter = new LazyInitSupplier<>(() -> {
        FutureConverter converter = new FutureConverter(getExecutor());

        futureConverterThread = new Thread(converter, "Mango FutureConverter loop");
        futureConverterThread.setUncaughtExceptionHandler(new MangoUncaughtExceptionHandler());
        futureConverterThread.start();

        return converter;
    });

    @Autowired
    public MangoExecutors(Environment env, RunAs runAs) {
        int scheduledThreadPoolSize = env.getProperty("executors.scheduled.corePoolSize", Integer.class, 1);

        this.scheduledExecutor = new ScheduledThreadPoolExecutor(scheduledThreadPoolSize, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "Mango shared scheduled executor");
                thread.setDaemon(true);
                thread.setContextClassLoader(Common.getModuleClassLoader());
                return thread;
            }
        });

        this.delegatingExecutor = new DelegatingSecurityContextExecutorService(executor);
        this.superadminExecutor = runAs.executorService(runAs.systemSuperadmin(), executor);
        this.delegatingScheduledExecutor = new DelegatingSecurityContextScheduledExecutorService(scheduledExecutor);
        this.superadminScheduledExecutor = runAs.scheduledExecutorService(runAs.systemSuperadmin(), scheduledExecutor);
    }

    @PreDestroy
    private void destroy() {
        if (log.isInfoEnabled()) {
            log.info("Shutting down shared executor and scheduled executor");
        }

        scheduledExecutor.shutdown();
        executor.shutdown();

        // interrupt the FutureConverter thread
        Thread futureConverterThread = this.futureConverterThread;
        if (futureConverterThread != null) {
            futureConverterThread.interrupt();
        }

        awaitTermination(Arrays.asList(scheduledExecutor, executor));
    }

    /**
     * Waits for termination of a collection of executors. If the tasks do no complete in a timely manner, shutdownNow() is called, interrupting the running tasks.
     */
    private void awaitTermination(Collection<ExecutorService> executors) {
        try {
            for (ExecutorService executor : executors) {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    if (log.isWarnEnabled()) {
                        log.warn("Executor's tasks did not complete in a timely manner, interrupting. {}", executor.toString());
                    }
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            if (log.isErrorEnabled()) {
                log.error("Interrupted while awaiting executor termination, interrupting all running tasks and exiting");
            }

            for (ExecutorService executor : executors) {
                executor.shutdownNow();
            }

            return;
        }

        // wait for termination again after shutdownNow() has been called on each executor that didn't terminate in a timely manner
        try {
            for (ExecutorService executor : executors) {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    if (log.isErrorEnabled()) {
                        log.error("Executor's tasks did not complete in a timely manner even after interrupting. {}", executor.toString());
                    }
                }
            }
        } catch (InterruptedException e) {
            if (log.isErrorEnabled()) {
                log.error("Interrupted while awaiting executor termination, exiting");
            }
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Executors shutdown successfully");
        }
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return this.delegatingScheduledExecutor;
    }

    public ExecutorService getExecutor() {
        return this.delegatingExecutor;
    }

    public ScheduledExecutorService getSuperadminScheduledExecutor() {
        return this.superadminScheduledExecutor;
    }

    public ExecutorService getSuperadminExecutor() {
        return this.superadminExecutor;
    }

    public <T> CompletableFuture<T> makeCompletable(Future<T> future) {
        return futureConverter.get().submit(future, 0, null);
    }

    public <T> CompletableFuture<T> makeCompletable(Future<T> future, long timeout, TimeUnit timeoutUnit) {
        return futureConverter.get().submit(future, timeout, timeoutUnit);
    }

}
