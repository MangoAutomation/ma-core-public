/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 * @author Terry Packer
 *
 */
public class OrderedRealTimeTimerTest {

    @BeforeClass
    public static void staticSetup() throws IOException{
        //Setup Mango properties Provider as we indirectly access Common
        Providers.add(MangoProperties.class, new MockMangoProperties());
    }

    /**
     * Test a 5ms period scheduled task that takes 30ms to execute
     *   ensure that it only runs queue size + 1 number of times
     */
    @Test
    public void testScheduledTask() {

        int taskPeriod = 5;
        int totalExecutions = 200;
        int taskQueueSize = 5;

        SimulationTimer simTimer = new SimulationTimer(true);
        ThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
                1,
                100,
                30L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                false,
                simTimer.getTimeSource());

        simTimer.init(executor);

        AtomicInteger runs = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        FixedRateTrigger trigger = new FixedRateTrigger(taskPeriod, taskPeriod);
        TimerTask task = new TimerTask(trigger, "task1", "1", taskQueueSize) {

            @Override
            public void run(long runtime) {
                runs.incrementAndGet();
                //If this is un-commented we can see that the main test thread will not yeild
                // until it simulates all time.  This will show that all 6 executions will happen
                // at time 1000 while all the rejections happen first.
                //System.out.println("Run: " + runs.get());
                //System.out.println("Runtime: " + runtime);
                //System.out.println("Walltime: " + simTimer.currentTimeMillis());
                //System.out.println("");
                try { Thread.sleep(6*taskPeriod); } catch (InterruptedException e) { }
            }

            @Override
            public void rejected(RejectedTaskReason reason) {
                rejections.incrementAndGet();
                //System.out.println("Rejected: " + reason.getDescription() +  " @ " + simTimer.currentTimeMillis());
            }
        };

        //Schedule the task
        simTimer.schedule(task);

        //Advance simulation time
        long time = taskPeriod;
        for(int i=0; i<totalExecutions; i++) {
            simTimer.fastForwardTo(time);
            time+=taskPeriod;
        }

        int waits = 10;
        while(waits > 0) {
            if(runs.get() + rejections.get() == totalExecutions)
                break;
            try { Thread.sleep(100); }catch(InterruptedException e) {  }
            waits--;
        }
        if(waits == 0)
            Assert.fail("Didn't execute all scheduled tasks.");
        Assert.assertEquals(totalExecutions, runs.get() + rejections.get());
        Assert.assertEquals(taskQueueSize + 1, runs.get());
        Assert.assertEquals(totalExecutions - (taskQueueSize + 1), rejections.get());

    }

    @Test
    public void testSubmittingTasksNoQueueing() {

        int taskDuration = 5;
        int totalExecutions = 20;
        int taskQueueSize = 5;

        SimulationTimer simTimer = new SimulationTimer(true);
        ThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
                1,
                100,
                30L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                false,
                simTimer.getTimeSource());

        simTimer.init(executor);

        AtomicInteger runs = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        //Advance simulation time
        long time = 5;
        for(int i=0; i<totalExecutions; i++) {
            OneTimeTrigger trigger = new OneTimeTrigger(new Date(simTimer.currentTimeMillis()));
            TimerTask task = new TimerTask(trigger, "task1", "1", taskQueueSize) {

                @Override
                public void run(long runtime) {
                    runs.incrementAndGet();
                    try { Thread.sleep(taskDuration); } catch (InterruptedException e) { }
                }

                @Override
                public void rejected(RejectedTaskReason reason) {
                    rejections.incrementAndGet();
                }
            };
            simTimer.schedule(task);
            simTimer.fastForwardTo(time);
            time+=100;
            try { Thread.sleep(100); }catch(InterruptedException e) {  }
        }

        int waits = 10;
        while(waits > 0) {
            if(runs.get() + rejections.get() == totalExecutions)
                break;
            try { Thread.sleep(50); }catch(InterruptedException e) {  }
            waits--;
        }
        if(waits == 0)
            Assert.fail("Didn't execute all scheduled tasks.");
        Assert.assertEquals(totalExecutions, runs.get() + rejections.get());
        Assert.assertEquals(totalExecutions, runs.get());
        Assert.assertEquals(0, rejections.get());

    }

    @Test
    public void testSubmittingTasksWithQueueing() {

        int taskDuration = 50;
        int totalExecutions = 10;
        int taskQueueSize = 5;

        SimulationTimer simTimer = new SimulationTimer(true);
        ThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
                1,
                100,
                30L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                false,
                simTimer.getTimeSource());

        simTimer.init(executor);

        AtomicInteger runs = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        //Advance simulation time
        long time = 5;
        for(int i=0; i<totalExecutions; i++) {
            OneTimeTrigger trigger = new OneTimeTrigger(new Date(simTimer.currentTimeMillis()));
            TimerTask task = new TimerTask(trigger, "task1", "1", taskQueueSize) {

                @Override
                public void run(long runtime) {
                    runs.incrementAndGet();
                    try { Thread.sleep(taskDuration); } catch (InterruptedException e) { }
                }

                @Override
                public void rejected(RejectedTaskReason reason) {
                    rejections.incrementAndGet();
                }
            };
            simTimer.schedule(task);
            simTimer.fastForwardTo(time);
            time+=100;
        }

        int waits = 10;
        while(waits > 0) {
            if(runs.get() + rejections.get() == totalExecutions)
                break;
            try { Thread.sleep(100); }catch(InterruptedException e) {  }
            waits--;
        }
        if(waits == 0)
            Assert.fail("Didn't execute all scheduled tasks.");
        Assert.assertEquals(totalExecutions, runs.get() + rejections.get());
        Assert.assertEquals(taskQueueSize + 1, runs.get());
        Assert.assertEquals(totalExecutions - (taskQueueSize + 1), rejections.get());

    }

    /**
     * Test multiple threads submitting the same task
     */
    @Test
    public void testMultipleSubmitThreads() {

        int executionsPerThread = 100;
        int threads = 5;
        int taskDuration = 50;
        int queueSize = 5;

        SimulationTimer simTimer = new SimulationTimer(true);
        ThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
                5,
                100,
                30L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                false,
                simTimer.getTimeSource());

        simTimer.init(executor);

        AtomicInteger runs = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        List<Integer> taskOrder = new ArrayList<>();
        AtomicInteger taskOrderId = new AtomicInteger();

        for(int i=0; i<threads; i++) {
            new Thread() {
                @Override
                public void run() {
                    for(int j=0; j<executionsPerThread; j++) {
                        Task task = new Task("task1", "1", queueSize) {
                            @Override
                            public void run(long runtime) {
                                runs.incrementAndGet();
                                taskOrder.add(taskOrderId.incrementAndGet());
                                try { Thread.sleep(taskDuration); } catch (InterruptedException e) { }
                            }

                            @Override
                            public void rejected(RejectedTaskReason reason) {
                                rejections.incrementAndGet();
                            }
                        };
                        simTimer.execute(task);
                    }
                };
            }.start();
        }

        int totalExecutions = threads * executionsPerThread;
        int waits = 10;
        while(waits > 0) {
            if(runs.get() + rejections.get() == totalExecutions)
                break;
            try { Thread.sleep(100); }catch(InterruptedException e) {  }
            waits--;
        }
        if(waits == 0)
            Assert.fail("Didn't execute all scheduled tasks.");

        Assert.assertEquals(totalExecutions, runs.get() + rejections.get());
        Assert.assertEquals(queueSize + 1, runs.get());

        //Ensure order
        Assert.assertEquals(1, (int)taskOrder.get(0));
        Assert.assertEquals(2, (int)taskOrder.get(1));
        Assert.assertEquals(3, (int)taskOrder.get(2));
        Assert.assertEquals(4, (int)taskOrder.get(3));
        Assert.assertEquals(5, (int)taskOrder.get(4));
        Assert.assertEquals(6, (int)taskOrder.get(5));
    }

    /*
     * Simulate a timeout task getting rejected and then throwing an exception
     */
    @Test
    public void testTimerThreadRejectionExceptions() throws InterruptedException {
        OrderedRealTimeTimer timer = new OrderedRealTimeTimer();
        OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
                0,
                3,
                30L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                false,
                timer.getTimeSource());
        timer.init(executor, Thread.MAX_PRIORITY);
        final AtomicLong time = new AtomicLong();

        //TODO
        //Track rejections
        //Track executions

        List<Task> tasks = new ArrayList<>();
        for(int i=0; i<3; i++) {
            tasks.add(new Task("TaskName", "TaskId" + i, 0) {

                @Override
                public void run(long runtime) {
                    try {Thread.sleep(1000); }catch(InterruptedException e) { }
                }

                @Override
                public void rejected(RejectedTaskReason reason) {

                }
            });
        }

        //Startup a new thread that inserts the blocking tasks
        new Thread() {
            @Override
            public void run() {

                for(Task task : tasks) {
                    TaskWrapper wrapper = new TaskWrapper(task, time.getAndAdd(100));
                    executor.execute(wrapper);
                }
            };
        }.start();

        //Execute the timeout task to be rejected
        new com.serotonin.m2m2.util.timeout.TimeoutTask(
                new OneTimeTrigger(new Date()), new TimeoutClient() {

                    @Override
                    public void scheduleTimeout(long fireTime) {
                        fail("Should not run");
                    }

                    @Override
                    public String getThreadName() {
                        return "Testing";
                    }

                    @Override
                    public void rejected(RejectedTaskReason reason) {
                        //Throw exception here and ensure it doesn't kill the timer thread
                        throw new RuntimeException("Purposeful rejected exception.");
                    }
                }, timer);

        Thread.sleep(500);
        //This will be set to false if the Timer Thread Fails
        Assert.assertEquals(timer.thread.newTasksMayBeScheduled, true);
        //TODO Cleanup and shutdown timer?
    }

}
