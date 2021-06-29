/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components.executors;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Continuously runs a loop that retrieves a {@link java.util.concurrent.Future} from a queue and checks if it is complete.
 * If it is complete the corresponding {@link java.util.concurrent.CompletableFuture} which was returned upon submission is completed.
 *
 * @author Jared Wiltshire
 */
class FutureConverter implements Runnable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final long pollInterval = 1;
    private final TimeUnit pollIntervalUnit = TimeUnit.MILLISECONDS;
    private final LinkedBlockingQueue<ConversionJob<Object>> queue = new LinkedBlockingQueue<>();
    private final Executor executor;

    /**
     * @param executor used to complete the CompletableFutures
     */
    FutureConverter(Executor executor) {
        this.executor = executor;
    }

    private static class ConversionJob<T> {
        Future<T> future;
        CompletableFuture<T> completableFuture;
        Long retries;
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> submit(Future<T> future, long timeout, TimeUnit timeoutUnit) {
        ConversionJob<T> job = new ConversionJob<T>();
        job.completableFuture = new CompletableFuture<>();
        job.future = future;
        if (timeout >= 0 && timeoutUnit != null) {
            job.retries = timeoutUnit.toMicros(timeout) / pollIntervalUnit.toMicros(pollInterval);
        }
        queue.add((ConversionJob<Object>) job);
        return job.completableFuture;
    }

    @Override
    public void run() {
        while (true) {
            try {
                ConversionJob<Object> job;
                try {
                    job = queue.take();
                } catch (InterruptedException e) {
                    // thread interrupted (terminated)
                    // nothing in the queue so just break the loop
                    break;
                }

                Object result = null;
                Throwable exception = null;

                try {
                    result = job.future.get(pollInterval, pollIntervalUnit);
                } catch (ExecutionException e) {
                    exception = e.getCause();
                } catch (CancellationException e) {
                    exception = e;
                } catch (TimeoutException e) {
                    if (job.retries != null && --job.retries <= 0) {
                        exception = e;
                    } else {
                        // get timeout exceeded, add back to queue for retry later
                        queue.add(job);
                        continue;
                    }
                } catch (InterruptedException e) {
                    // thread interrupted (terminated)
                    // complete all the CompletableFutures from this thread and break the loop

                    job.completableFuture.completeExceptionally(e);
                    while ((job = queue.poll()) != null) {
                        job.completableFuture.completeExceptionally(e);
                    }
                    break;
                }

                try {
                    // dont ever want to complete the Future in our loop thread
                    completeFutureAsync(job.completableFuture, result, exception);
                } catch (RejectedExecutionException e) {
                    // retry later
                    queue.add(job);
                }

            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Error in CompletableFuture conversion loop, jobs may have been lost", e);
                }
            }
        }
    }

    private void completeFutureAsync(CompletableFuture<Object> future, Object result, Throwable exception) {
        if (executor != null) {
            executor.execute(() -> {
                completeFuture(future, result, exception);
            });
        } else {
            completeFuture(future, result, exception);
        }
    }

    private void completeFuture(CompletableFuture<Object> future, Object result, Throwable exception) {
        if (exception != null) {
            future.completeExceptionally(exception);
        } else {
            future.complete(result);
        }
    }
}