/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import static org.junit.Assert.fail;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.timer.OrderedThreadPoolExecutor.OrderedTaskQueue;

/**
 *
 * @author Terry Packer
 */
public class OrderedThreadPoolExecutorTest {
    

    boolean printResults = false;
    
    /**
     * Test inserting tasks faster than they can be run and ensure they run in insertion order
     * @throws InterruptedException
     */
    @Test
    public void testFailedExecutions() throws InterruptedException {
        
        final long sleepMs = 2;
        boolean flushOnReject = false;
        final String taskId = "TSK_FAIL";
        final int taskCount = 100;
        final long timeStep = 1;
        final List<TestTask> toProcess = new ArrayList<>();
        final List<TestTask> processed = new ArrayList<>();
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
        for(int i=0; i<taskCount; i++)
            toProcess.add(new TestTask("Failure", taskId, Task.UNLIMITED_QUEUE_SIZE, i, true, -1, sleepMs, processed));
        
        //Start the threads to process the tasks and put into the executor so that they 
        //Starup a new thread that inserts failing tasks
        new Thread() {
            public void run() {
                
                for(TestTask task : toProcess) {
                    if(printResults)
                        System.out.println("Scheduling " + task.runId);
                    exe.execute(new TaskWrapper(task, time.getAndAdd(timeStep)));
                    try {
                        Thread.sleep(sleepMs/2);
                    } catch (InterruptedException e) {

                    }
                }
            };
        }.start();
        
        //Sleep to ensure all tasks ran and return if they did
        Thread.sleep(1000);
        int waitCount = taskCount;
        for(int i=0; i<waitCount; i++) {
            Thread.sleep(sleepMs);
            if(!exe.queueExists(taskId))
                break;
        }

        //Did we run all the tasks?
        OrderedTaskQueue queue = exe.getTaskQueue(taskId);
        if(queue != null)
            fail("non empty queue");
        
        //Check for a pool rejection
        Assert.assertEquals(poolRejection.get(), false);
        
        //Check again to make sure they actually ran
        Assert.assertEquals(toProcess.size(), processed.size());
        
        for(int i=0; i<toProcess.size(); i++) {
            Assert.assertEquals(toProcess.get(i).runId, processed.get(i).runId);
        }
        
        //Check for rejection failures
        for(TestTask test : toProcess) {
            if(printResults)
                System.out.println(test.toString());
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
    public void testQueueFullRejectedExecutions() throws InterruptedException {

        boolean flushOnReject = false;
        final long sleepMs = 100;
        final String taskId = "TSK_FAIL";
        final int taskCount = 20;
        final int queueSize = 10;
        final long timeStep = 1;
        final List<TestTask> toProcess = new ArrayList<>();
        final List<TestTask> processed = new ArrayList<>();
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
        for(int i=0; i<taskCount; i++)
            toProcess.add(new TestTask("Failure", taskId, queueSize, i, false, i > queueSize - 1 ? RejectedTaskReason.TASK_QUEUE_FULL : -1, sleepMs, processed));
        
        //Start the threads to process the tasks and put into the executor so that they 
        //Starup a new thread that inserts failing tasks
        new Thread() {
            public void run() {
                
                for(TestTask task : toProcess) {
                    if(printResults)
                        System.out.println("Scheduling " + task.runId);
                    exe.execute(new TaskWrapper(task, time.getAndAdd(timeStep)));
                }
            };
        }.start();
        
        //Sleep to ensure all tasks ran and return if they did
        Thread.sleep(1000);
        int waitCount = taskCount;
        for(int i=0; i<waitCount; i++) {
            Thread.sleep(sleepMs);
            if(!exe.queueExists(taskId))
                break;
        }

        //Did we run all the tasks?
        OrderedTaskQueue queue = exe.getTaskQueue(taskId);
        if(queue != null)
            fail("non empty queue");
        
        //Check for a pool rejection
        Assert.assertEquals(poolRejection.get(), false);
        
        //Check again to make sure they actually ran (a full queue + the first task)
        Assert.assertEquals(queueSize + 1, processed.size());
        
        //Check processed tasks
        for(int i=0; i<processed.size(); i++) {
            Assert.assertEquals(toProcess.get(i).runId, processed.get(i).runId);
        }

        //Check for rejection failures
        for(TestTask test : toProcess) {
            if(printResults)
                System.out.println(test.toString());
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
        final long sleepMs = 500;
        final String taskId = "TSK_FAIL";
        final int taskCount = 20;
        final int poolSize = 3;
        final long timeStep = 1;
        final List<TestTask> toProcess = new ArrayList<>();
        final List<TestTask> processed = new ArrayList<>();
        final AtomicLong time = new AtomicLong();
        final AtomicBoolean poolRejection = new AtomicBoolean();

        IgnoreExpectedException handler = new IgnoreExpectedException();
        
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
                       if(printResults)
                           System.out.println("Rejected" + wrapper);
                       poolRejection.set(true);
                    }
                },
                flushOnReject,
                new SystemTimeSource());
        
        //Generate tasks to run in order
        for(int i=0; i<taskCount; i++)
            toProcess.add(new TestTask("Failure", taskId + i, 0, i, false, i > poolSize ? RejectedTaskReason.POOL_FULL : -1, sleepMs, processed));
        
        //Start the threads to process the tasks and put into the executor so that they 
        //Starup a new thread that inserts failing tasks
        new Thread() {
            public void run() {
                
                for(TestTask task : toProcess) {
                    if(printResults)
                        System.out.println("Scheduling " + task.runId);
                    exe.execute(new TaskWrapper(task, time.getAndAdd(timeStep)));
                }
            };
        }.start();
        
        //Sleep to ensure all tasks ran and return if they did
        Thread.sleep(1000);
        int waitCount = poolSize;
        for(int i=0; i<waitCount; i++) {
            Thread.sleep(sleepMs);
        }

        //Did we run all the tasks?
        OrderedTaskQueue queue = exe.getTaskQueue(taskId);
        if(queue != null)
            fail("non empty queue");
        
        //Check for a pool rejection
        Assert.assertEquals(true, poolRejection.get());
        
        //Check again to make sure they actually ran 
        Assert.assertEquals(poolSize, processed.size());
        
        for(TestTask test : toProcess) {
            if(printResults)
                System.out.println(test.toString());
            if(test.rejectionFailure())
                fail(test.rejectionFailureDescription);
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
    
    class TestTask extends Task {

        final int runId;
        final boolean throwException;
        final long sleepMs;
        final int rejectCode;
        final List<TestTask> processed;
        long runtime = -1;
        String rejectedDescription = null;
        String rejectionFailureDescription = null;
        
        
        
        /**
         * @param name
         * @param id
         * @param queueSize
         */
        public TestTask(String name, String id, int queueSize, int runId, boolean throwException, int rejectCode, long sleepMs,  final List<TestTask> processed) {
            super(name, id, queueSize);
            this.runId = runId;
            this.sleepMs = sleepMs;
            this.rejectCode = rejectCode;
            this.throwException = throwException;
            this.processed = processed;
        }
        
        @Override
        public void run(long runtime) {
            this.runtime = runtime;
            this.processed.add(this);
            if(printResults)
                System.out.println("running task " + runId);
            try { Thread.sleep(sleepMs); } catch (InterruptedException e) { }
            if(throwException)
                throw new ExpectedException();
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            if(reason.getCode() != rejectCode)
                rejectionFailureDescription = "Task " + runId + " should not have been rejected for " + reason.getDescription();
            if(rejectedDescription != null)
                rejectionFailureDescription = "Task " + runId + " can only be rejected once.";
            rejectedDescription = reason.getDescription();
        }
        
        public boolean rejectionFailure() {
            return rejectionFailureDescription != null;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
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
    
}
