/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Terry Packer
 *
 */
public class CurrentThreadExecutorService implements ExecutorService {

    boolean shutdown = false;
    
    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = false;
        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return new CurrentThreadFuture<T>(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return new CurrentThreadFuture<T>(()-> {
            task.run();
            return result;
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        return new CurrentThreadFuture<Void>(()-> {
            task.run();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>();
        for(Callable<?> t : tasks) {
            futures.add(new CurrentThreadFuture<T>((Callable<T>)t));
        }
        return futures;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>();
        for(Callable<?> t : tasks) {
            futures.add(new CurrentThreadFuture<T>((Callable<T>)t));
        }
        return futures;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        for(Callable<?> t : tasks) {
            try {
                return (T) t.call();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        for(Callable<?> t : tasks) {
            try {
                return (T) t.call();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public static class CurrentThreadFuture<T> implements Future<T> {

        boolean cancelled;
        boolean done;
        Callable<T> task;
        
        public CurrentThreadFuture(Callable<T> task) {
            this.task = task;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return cancelled;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                T result = task.call();
                done = true;
                return result;
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            try {
                T result = task.call();
                done = true;
                return result;
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }
        
    };

}
