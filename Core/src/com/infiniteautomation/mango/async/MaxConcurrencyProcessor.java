/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.async;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * @author Jared Wiltshire
 * @param <T> input type
 * @param <R> result type
 */
public final class MaxConcurrencyProcessor<T, R> {
    static class QueuedItem<T, R> {
        final CompletableFuture<R> future = new CompletableFuture<>();
        final T item;

        public QueuedItem(T item) {
            this.item = item;
        }
    }

    final Queue<QueuedItem<T, R>> queue = new ConcurrentLinkedQueue<>();
    final Semaphore semaphore;
    final ExecutorService executorService;
    final Function<T, R> function;

    public MaxConcurrencyProcessor(Function<T, R> function, int maxConcurrency, ExecutorService executorService) {
        this.function = function;
        this.semaphore = new Semaphore(maxConcurrency);
        this.executorService = executorService;
    }

    public CompletionStage<R> add(T item) {
        QueuedItem<T, R> queuedItem = new QueuedItem<>(item);
        queue.add(queuedItem);

        boolean acquired = semaphore.tryAcquire();
        if (acquired) {
            try {
                executorService.execute(this::processQueue);
            } catch (RejectedExecutionException e) {
                semaphore.release();
                throw e;
            }
        }

        return queuedItem.future;
    }

    private void processQueue() {
        try {
            QueuedItem<T, R> item;
            while ((item = queue.poll()) != null) {
                try {
                    R result = function.apply(item.item);
                    item.future.complete(result);
                } catch (Exception e) {
                    item.future.completeExceptionally(e);
                }
            }
        } finally {
            semaphore.release();
        }
    }
}
