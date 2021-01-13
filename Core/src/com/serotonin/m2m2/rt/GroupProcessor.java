/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public abstract class GroupProcessor<T,R> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Executor executor;
    protected final int maxConcurrency;
    protected final Semaphore semaphore;

    public GroupProcessor(Executor executor, int maxConcurrency) {
        this.executor = executor;
        this.maxConcurrency = maxConcurrency;
        this.semaphore = new Semaphore(maxConcurrency);
    }

    public List<R> process(List<T> items) {
        try {
            List<CompletableFuture<R>> futures = new ArrayList<>(items.size());
            int itemId = 0;
            for (T item : items) {
                futures.add(submit(item, itemId++));
            }

            List<R> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                Future<R> f = futures.get(i);
                try {
                    results.add(f.get());
                } catch (ExecutionException e) {
                    if (log.isErrorEnabled()) {
                        log.error("Item failed, continuing. Item: {}", items.get(i), e);
                    }
                }
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<R> submit(T item, int itemId) throws InterruptedException {
        try {
            semaphore.acquire();
            CompletableFuture<R> future = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    future.complete(processItem(item, itemId));
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                } finally {
                    semaphore.release();
                }
            });
            return future;
        } catch (RejectedExecutionException e) {
            semaphore.release();
            throw e;
        }
    }

    protected abstract R processItem(T t, int itemId) throws Exception;
}
