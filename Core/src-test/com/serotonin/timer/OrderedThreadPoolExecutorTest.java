/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.timer.OrderedThreadPoolExecutor.LimitedTaskQueue;

/**
 *
 * @author Terry Packer
 */
public class OrderedThreadPoolExecutorTest {
    
    final long sleepMs = 2;
    boolean printResults = false;
    
    /**
     * Test inserting tasks faster than they can be run and ensure they run in insertion order
     * @throws InterruptedException
     */
    @Test
    public void testFailedExecutions() throws InterruptedException {

        boolean flushOnReject = false;
        final String taskId = "TSK_FAIL";
        final int taskCount = 100;
        final long timeStep = 1;
        final List<TestTask> toProcess = new ArrayList<>();
        final List<TestTask> processed = new ArrayList<>();
        final AtomicLong time = new AtomicLong();
        
        OrderedThreadPoolExecutor exe = new OrderedThreadPoolExecutor(
                0,
                3,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new MangoThreadFactory("medium", Thread.MAX_PRIORITY - 2),
                new RejectedExecutionHandler() {
                    
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                       System.out.println("Rejected.");
                       fail("Should never reject tasks from the pool in this configuration");
                    }
                },
                flushOnReject,
                new SystemTimeSource());
        
        //Generate tasks to run in order
        for(int i=0; i<taskCount; i++)
            toProcess.add(new TestTask("Failure", taskId, Task.UNLIMITED_QUEUE_SIZE, i, processed));
        
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
        int waitCount = taskCount + 100;
        for(int i=0; i<waitCount; i++) {
            Thread.sleep(sleepMs);
            if(!exe.queueExists(taskId))
                break;
        }

        //Did we run all the tasks?
        LimitedTaskQueue queue = exe.getTaskQueue(taskId);
        if(queue != null)
            fail("non empty queue");
        
        //Check again to make sure they actually ran
        Assert.assertEquals(toProcess.size(), processed.size());
        
        for(int i=0; i<toProcess.size(); i++) {
            Assert.assertEquals(toProcess.get(i).runId, processed.get(i).runId);
        }
        
        //Not all tasks finished, why?
        if(printResults) {
            for(TestTask test : processed)
                System.out.println(test.toString());
        }
    }
    
    class TestTask extends Task {

        final int runId;
        long runtime = -1;
        final List<TestTask> processed;
        
        /**
         * @param name
         * @param id
         * @param queueSize
         */
        public TestTask(String name, String id, int queueSize, int runId, final List<TestTask> processed) {
            super(name, id, queueSize);
            this.runId = runId;
            this.processed = processed;
        }
        
        @Override
        public void run(long runtime) {
            this.runtime = runtime;
            this.processed.add(this);
            if(printResults)
                System.out.println("running task " + runId);
            try { Thread.sleep(sleepMs); } catch (InterruptedException e) { }
            //throw new RuntimeException("Purposeful exception");
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
           System.out.println("Task " + runId + " Rejected" );
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            if(runtime < 0)
                return "Task " + runId + " [did not run]";
            else
                return "Task " + runId + " [" + runtime + "]";
        }
        
    }
    
}
