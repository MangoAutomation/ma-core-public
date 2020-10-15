/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public abstract class GroupProcessor<T,R> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final LimitedConcurrencyExecutor executor;
    protected final int maxConcurrency;

    /**
     * @param priorityList
     */
    public GroupProcessor(ExecutorService executor, int maxConcurrency) {
        this.executor = new LimitedConcurrencyExecutor(executor, maxConcurrency);
        this.maxConcurrency = maxConcurrency;
    }

    public List<R> process(List<T> items) {
        List<CompletableFuture<R>> futures = new ArrayList<>(items.size());
        for (T item : items) {
            futures.add(executor.submit(() -> processItem(item)));
        }

        List<R> results = new ArrayList<>();
        for (Future<R> f : futures) {
            try {
                results.add(f.get());
            } catch (ExecutionException | CancellationException e) {
                log.error("Item failed, continuing", e);
            } catch (InterruptedException e) {
                log.error("Interrupted, aborting immediately", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return results;
    }

    protected abstract R processItem(T t) throws Exception;
}
