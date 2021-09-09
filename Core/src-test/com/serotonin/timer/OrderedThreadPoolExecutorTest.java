/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import static org.junit.Assert.fail;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.timer.OrderedThreadPoolExecutor.OrderedTaskQueue;

/**
 *
 * @author Terry Packer
 */
public class OrderedThreadPoolExecutorTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    static {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.DEBUG);
    }
    
    /**
     * Test inserting tasks faster than they can be run and ensure they run in insertion order
     * @throws InterruptedException
     */
    @Test(timeout = 60 * 1000 * 3)
    public void testFailedExecutions() throws InterruptedException {

        boolean flushOnReject = false;
        final String taskId = "TSK_FAIL";
        final int taskCount = 100;
        final long timeStep = 1;
        final List<TestTask> tasks = new ArrayList<>();
        final Queue<Integer> executionOrder = new LinkedList<>();
        final AtomicLong time = new AtomicLong();
        final AtomicBoolean poolRejection = new AtomicBoolean();
        final AtomicInteger tasksProcessed = new AtomicInteger();
        final IgnoreExpectedException handler = new IgnoreExpectedException();

        OrderedThreadPoolExecutor exe = new OrderedThreadPoolExecutor(
                0,
                3,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new MangoThreadFactory("medium", Thread.MAX_PRIORITY - 2, Thread.currentThread().getContextClassLoader(), handler),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                       poolRejection.set(true);
                       tasksProcessed.getAndIncrement();
                    }
                },
                flushOnReject,
                new SystemTimeSource());
        
        //Generate tasks to run in order
        CompletableFuture[] all = new CompletableFuture[taskCount];
        for(int i=0; i<taskCount; i++) {
            AtomicInteger tId = new AtomicInteger(i);
            TestTask task = new TestTask("Failure", taskId, Task.UNLIMITED_QUEUE_SIZE, i, true,
                    -1, i, () -> {
                executionOrder.add(tId.get());
            });
            all[i] = task.getWork();
            tasks.add(task);
        }

        //Execute the tasks
        for(TestTask task : tasks) {
            log.debug("Scheduling " + task.runId);
            exe.execute(new TaskWrapper(task, time.getAndAdd(timeStep)));
        }

        //Ensure all tasks ran
        try {
            CompletableFuture.allOf(all).get();
        } catch (ExecutionException e) {
            log.error("Completed Exceptionally", e);
            fail(e.getMessage());
        }

        //Did we run all the tasks?
        OrderedTaskQueue queue = exe.getTaskQueue(taskId);
        if(queue != null && !queue.isEmpty())
            fail("non empty queue");
        
        //Check for a pool rejection
        Assert.assertEquals(poolRejection.get(), false);
        
        //Check again to make sure they actually ran
        int ran = 0;
        try {
            for (TestTask task : tasks) {
                if (task.getWork().get() == null) {
                    ran++;
                }
            }
        } catch (ExecutionException e) {
            log.error("Failed to get result", e);
            fail(e.getMessage());
        }
        Assert.assertEquals(tasks.size(), ran);

        AtomicInteger order = new AtomicInteger();
        executionOrder.stream().forEach(o -> {
            Assert.assertEquals((long)order.getAndIncrement(), (long)o);
        });
        
        //Check for rejection failures
        for(TestTask test : tasks) {
            log.debug(test.toString());
            if(test.rejectionFailure())
                fail(test.rejectionFailureDescription);
        }

        handler.throwIfNotEmpty();
    }

    @Test
    public void testQueueFullRejectedExecutions() throws InterruptedException {

        boolean flushOnReject = false;
        final String taskId = "TSK_FAIL";
        final int taskCount = 20;
        final int queueSize = 10;
        final long timeStep = 1;
        final List<TestTask> tasks = new ArrayList<>();
        final Queue<Integer> executionOrder = new LinkedList<>();
        final AtomicLong time = new AtomicLong();
        final AtomicBoolean poolRejection = new AtomicBoolean();

        IgnoreExpectedException handler = new IgnoreExpectedException();

        OrderedThreadPoolExecutor exe = new OrderedThreadPoolExecutor(
                0,
                3,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new MangoThreadFactory("medium", Thread.MAX_PRIORITY - 2, Thread.currentThread().getContextClassLoader(), handler),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        poolRejection.set(true);
                    }
                },
                flushOnReject,
                new SystemTimeSource());

        //Generate tasks to run in order
        CompletableFuture[] all = new CompletableFuture[taskCount];
        List<CompletableFuture> waitingOn = new ArrayList<>();
        for(int i=0; i<taskCount; i++) {

            TestTask task;
            if(i > queueSize -1) {
                task = new TestTask("Failure", taskId, queueSize, i, false,
                        RejectedTaskReason.TASK_QUEUE_FULL, i);
            }else {
                AtomicInteger tId = new AtomicInteger(i);
                task = new DelayedTestTask("Should run", taskId, queueSize, i, false,
                        -1, () -> {
                    executionOrder.add(tId.get());
                });
                waitingOn.add(task.getWork());
            }
            all[i] = task.getWork();
            tasks.add(task);
        }

        //Submit the tasks
        for(TestTask task : tasks) {
            log.debug("Scheduling " + task.runId);
            exe.execute(new TaskWrapper(task, time.getAndAdd(timeStep)));
        }

        //All tasks necessary should have been rejected so we can
        //  release the waiting tasks
        for(CompletableFuture work : waitingOn) {
            work.complete(1);
        }

        //Ensure all tasks ran
        try {
            CompletableFuture.allOf(all).get();
        } catch (ExecutionException e) {
            log.error("Completed Exceptionally", e);
            fail(e.getMessage());
        }

        //Did we run all the tasks?
        OrderedTaskQueue queue = exe.getTaskQueue(taskId);
        if(queue != null && !queue.isEmpty()) {
            fail("non empty queue");
        }

        //Check for a pool rejection
        Assert.assertEquals(poolRejection.get(), false);

        //Check again to make sure they actually ran (a full queue + the first task)
        int ran = 0;
        try {
            for (TestTask task : tasks) {
                if (task.getWork().get() != null) {
                    ran++;
                }
            }
        } catch (ExecutionException e) {
            log.error("Failed to get result", e);
            fail(e.getMessage());
        }
        Assert.assertEquals(queueSize + 1, ran);

        //Check processed task order
        AtomicInteger order = new AtomicInteger();
        executionOrder.stream().forEach(o -> {
            Assert.assertEquals((long)order.getAndIncrement(), (long)o);
        });

        //Check for rejection failures
        for(TestTask test : tasks) {
            log.debug(test.toString());
            if(test.rejectionFailure())
                fail(test.rejectionFailureDescription);
        }

        handler.throwIfNotEmpty();
    }

    /**
     * Test inserting tasks faster than they can be run and ensure they run in insertion order
     * @throws InterruptedException
     */
    @Test
    public void testPoolFullRejectedExecutions() throws InterruptedException {

        boolean flushOnReject = false;
        final String taskId = "TSK_FAIL";
        final int taskCount = 20;
        final int poolSize = 3;
        final long timeStep = 1;
        final AtomicLong time = new AtomicLong();
        final AtomicBoolean poolRejection = new AtomicBoolean();

        IgnoreExpectedException handler = new IgnoreExpectedException();

        List<TestTask> tasks = new ArrayList<>();
        Map<Integer, CompletableFuture> futureMap = new HashMap<>();
        OrderedThreadPoolExecutor exe = new OrderedThreadPoolExecutor(
                0,
                poolSize,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new MangoThreadFactory("high", Thread.MAX_PRIORITY - 2, Thread.currentThread().getContextClassLoader(), handler),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        TaskWrapper wrapper = (TaskWrapper)r;
                        log.debug("Rejected " + wrapper.task);
                        poolRejection.set(true);
                        TestTask task = (TestTask)wrapper.getTask();
                        //Simulate completion (don't use exception)
                        task.getWork().complete(null);
                    }
                },
                flushOnReject,
                new SystemTimeSource());

        //Generate tasks to run in order
        CompletableFuture[] all = new CompletableFuture[taskCount];
        List<CompletableFuture> waitingOn = new ArrayList<>();
        for(int i=0; i<taskCount; i++) {
            TestTask task;
            if(i > poolSize) {
                task = new TestTask("Rejected", taskId + i, 0, i, false,
                        RejectedTaskReason.POOL_FULL, i);
            }else {
                task = new DelayedTestTask("Should Run", taskId + i, 0, i, false,
                        -1);
                waitingOn.add(task.getWork());
            }
            all[i] = task.getWork();
            tasks.add(task);
        }

        //Submit all tasks for processing
        for(TestTask task : tasks) {
            log.debug("Scheduling " + task.runId);
            exe.execute(new TaskWrapper(task, time.getAndAdd(timeStep)));
        }

        //All tasks necessary should have been rejected so we can
        //  release the waiting tasks
        for(CompletableFuture work : waitingOn) {
            work.complete(1);
        }

        //Ensure all tasks ran
        try {
            CompletableFuture.allOf(all).get();
        } catch (ExecutionException e) {
            log.error("Completed Exceptionally", e);
            fail(e.getMessage());
        }

        //Did we run all the tasks?
        OrderedTaskQueue queue = exe.getTaskQueue(taskId);
        if(queue != null && !queue.isEmpty()) {
            fail("non empty queue");
        }

        //Check for a pool rejection
        Assert.assertEquals(true, poolRejection.get());

        //Check again to make sure they actually ran
        int ran = 0;
        try {
            for (TestTask task : tasks) {
                if (task.getWork().get() != null) {
                    ran++;
                }
            }
        } catch (ExecutionException e) {
            log.error("Failed to get result", e);
            fail(e.getMessage());
        }
        Assert.assertEquals(poolSize, ran);

        for(TestTask test : tasks) {
            log.debug(test.toString());
            if(test.rejectionFailure()) {
                fail(test.rejectionFailureDescription);
            }
        }

        handler.throwIfNotEmpty();
    }


    public static class IgnoreExpectedException implements UncaughtExceptionHandler {

        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (!(e instanceof ExpectedException)) {
                exceptions.add(e);
            }
        }

        public List<Throwable> getExceptions() {
            return exceptions;
        }

        public void throwIfNotEmpty() {
            if (!exceptions.isEmpty()) {
                RuntimeException e = new RuntimeException("Uncaught exceptions in tasks");
                for (Throwable t : exceptions) {
                    e.addSuppressed(t);
                }
                throw e;
            }
        }
    }

    public static class ExpectedException extends RuntimeException {
        public ExpectedException() {
        }
    }
    
    class TestTask<T> extends Task {

        final int runId;
        final boolean throwException;
        final int rejectCode;
        final T result;
        final CompletableFuture<T> work;
        final Runnable r;
        long runtime = -1;
        String rejectedDescription = null;
        String rejectionFailureDescription = null;


        public TestTask(String name, String id, int queueSize, int runId, boolean throwException, int rejectCode,
                        T result) {
            this(name, id, queueSize, runId, throwException, rejectCode, result, new CompletableFuture<>(), null);
        }

        /**
         *
         * @param name
         * @param id
         * @param queueSize
         * @param runId
         * @param throwException
         * @param rejectCode
         * @param result
         * @param r - run at start of task
         */
        public TestTask(String name, String id, int queueSize, int runId, boolean throwException, int rejectCode,
                        T result, Runnable r) {
            this(name, id, queueSize, runId, throwException, rejectCode, result, new CompletableFuture<>(), r);
        }

        public TestTask(String name, String id, int queueSize, int runId, boolean throwException, int rejectCode,
                        T result,  CompletableFuture<T> work, Runnable r) {
            super(name, id, queueSize);
            this.runId = runId;
            this.rejectCode = rejectCode;
            this.result = result;
            this.throwException = throwException;
            this.work = work;
            this.r = r;
        }

        @Override
        public void run(long runtime) {
            this.runtime = runtime;
            log.debug("Running task " + runId);

            if(r != null) {
                r.run();
            }

            if(throwException) {
                ExpectedException e = new ExpectedException();
                this.work.complete(null);
                throw e;
            }else {
                this.work.complete(result);
            }
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            if(reason.getCode() != rejectCode)
                rejectionFailureDescription = "Task " + runId + " should not have been rejected for " + reason.getDescription();
            if(rejectedDescription != null)
                rejectionFailureDescription = "Task " + runId + " can only be rejected once.";
            rejectedDescription = reason.getDescription();
            //Complete work but don't use exception
            work.complete(null);
        }
        
        public boolean rejectionFailure() {
            return rejectionFailureDescription != null;
        }

        public CompletableFuture<T> getWork() {
            return this.work;
        }

        @Override
        public String toString() {
            if(rejectionFailureDescription != null)
                return "Task " + runId + " [ " + rejectionFailureDescription + " ]";
            else if(rejectedDescription != null)
                return "Task " + runId + " [ " + rejectedDescription + " ]";
            else if(runtime > -1 )
                return "Task " + runId + " [ ran at time " + runtime + "]";
            else
                return "Task " + runId + " [did not run]";
        }
        
    }

    /**
     * Create a task that will wait in it's run method until it's future is completed.
     */
    class DelayedTestTask extends TestTask<Void> {
        public DelayedTestTask(String name, String id, int queueSize, int runId,
                               boolean throwException, int rejectCode) {
            super(name, id, queueSize, runId, throwException, rejectCode, null, null);
        }

        /**
         *
         * @param name
         * @param id
         * @param queueSize
         * @param runId
         * @param throwException
         * @param rejectCode
         * @param r - execute before waiting on the future to be completed
         */
        public DelayedTestTask(String name, String id, int queueSize, int runId,
                               boolean throwException, int rejectCode, Runnable r) {
            super(name, id, queueSize, runId, throwException, rejectCode, null, r);
        }

        @Override
        public void run(long runtime) {
            log.debug("Running task " + runId);
            this.runtime = runtime;
            if(r != null) {
                r.run();
            }
            try {
                this.work.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
    
}
