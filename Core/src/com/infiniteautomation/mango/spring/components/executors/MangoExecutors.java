/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components.executors;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.util.LazyInitSupplier;

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

    private static LazyInitSupplier<FutureConverter> futureConverter = new LazyInitSupplier<>(() -> {
        FutureConverter converter = new FutureConverter(ForkJoinPool.commonPool());
        Thread converterThread = new Thread(converter, "CompletableFuture conversion loop");
        converterThread.setDaemon(true);
        converterThread.start();
        return converter;
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

        Optional<FutureConverter> converter = futureConverter.getIfInitialized();
        if (converter.isPresent()) {
            try {
                converter.get().stop();
            } catch (InterruptedException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to FutureConverter service cleanly");
                }
            }
        }
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public <T> CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    public <T> CompletableFuture<T> makeCompletable(Future<T> future) {
        return futureConverter.get().submit(future, 0, null);
    }

    public <T> CompletableFuture<T> makeCompletable(Future<T> future, long timeout, TimeUnit timeoutUnit) {
        return futureConverter.get().submit(future, timeout, timeoutUnit);
    }
}
