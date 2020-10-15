/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Provides a way to limit concurrency of tasks submitted through this interface while delegating
 * to an existing Executor. When the limit is reached any calls made to
 * {@link #submit(java.util.concurrent.Callable)} will block until a task completes.
 *
 * @author Jared Wiltshire
 */
public final class LimitedConcurrencyExecutor {
    private final Executor delegate;
    private final Semaphore semaphore;

    public LimitedConcurrencyExecutor(Executor delegate, int maxConcurrency) {
        this.delegate = delegate;
        this.semaphore = new Semaphore(maxConcurrency);
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        Objects.requireNonNull(task);
        try {
            semaphore.acquire();
            CompletableFuture<T> future = new CompletableFuture<>();
            delegate.execute(() -> {
                try {
                    future.complete(task.call());
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                } finally {
                    semaphore.release();
                }
            });
            return future;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(e);
        } catch (RejectedExecutionException e) {
            semaphore.release();
            throw e;
        }
    }
}