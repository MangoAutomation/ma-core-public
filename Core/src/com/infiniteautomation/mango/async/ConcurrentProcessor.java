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
 * Queues items for processing, allows for a fixed number of concurrent operations at a time using a shared executor.
 *
 * @author Jared Wiltshire
 * @param <T> input type
 * @param <R> result type
 */
public final class ConcurrentProcessor<T, R> {
    private static class QueuedItem<T, R> {
        final CompletableFuture<R> future = new CompletableFuture<>();
        final T item;

        public QueuedItem(T item) {
            this.item = item;
        }
    }

    private final Queue<QueuedItem<T, R>> queue = new ConcurrentLinkedQueue<>();
    private final Semaphore semaphore;
    private final ExecutorService executorService;
    private final Function<T, R> function;

    public ConcurrentProcessor(Function<T, R> function, int maxConcurrency, ExecutorService executorService) {
        this.function = function;
        this.semaphore = new Semaphore(maxConcurrency);
        this.executorService = executorService;
    }

    /**
     * Adds an item to the queue for processing.
     *
     * @param item item to be processed
     * @return future that will be completed when item was processed
     */
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
                    // check if future was cancelled etc
                    if (!item.future.isDone()) {
                        R result = function.apply(item.item);
                        item.future.complete(result);
                    }
                } catch (Exception e) {
                    item.future.completeExceptionally(e);
                }
            }
        } finally {
            semaphore.release();
        }
    }
}
