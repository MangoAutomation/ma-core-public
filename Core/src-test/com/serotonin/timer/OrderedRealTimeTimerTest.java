/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.provider.Providers;
import com.serotonin.timer.OrderedThreadPoolExecutorTest.ExpectedException;
import com.serotonin.timer.OrderedThreadPoolExecutorTest.IgnoreExpectedException;
import com.serotonin.util.properties.MangoProperties;

/**
 * @author Terry Packer
 *
 */
public class OrderedRealTimeTimerTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    static {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.DEBUG);
    }

    @BeforeClass
    public static void staticSetup() {
        //Setup Mango properties Provider as we indirectly access Common
        Providers.add(MangoProperties.class, new MockMangoProperties());
    }

    @BeforeClass
    public static void useAdmin() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        MangoTestBase.setSuperadminAuthentication();
    }

    /**
     * Test a 5ms period scheduled task that takes 30ms to execute
     *   ensure that it only runs queue size + 1 number of times
     */
    @Test(timeout = 60 * 1000 * 3)
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
        AtomicInteger execution = new AtomicInteger();

        //Fill out the list of things to do
        CompletableFuture<Integer> firstTaskWork = new CompletableFuture<>();
        List<CompletableFuture> executions = new ArrayList<>();
        for(int i=1; i<totalExecutions; i++) {
            executions.add(new CompletableFuture());
        }

        FixedRateTrigger trigger = new FixedRateTrigger(taskPeriod, taskPeriod);
        TimerTask task = new TimerTask(trigger, "task1", "1", taskQueueSize) {

            @Override
            public void run(long runtime) {
                int run = runs.getAndIncrement();
                if(run >= 1) {
                    CompletableFuture<Integer> work = executions.get(execution.getAndIncrement());

                    //We can see that the main test thread will not yield
                    // until it simulates all time.  This will show that all 6 executions will happen
                    // at time 1000 while all the rejections happen first.
                    log.debug("Run: " + run);
                    log.debug("Runtime: " + runtime);
                    log.debug("Walltime: " + simTimer.currentTimeMillis());

                    long taskEndtime = runtime + (6 * taskPeriod);
                    while (simTimer.currentTimeMillis() < taskEndtime) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    work.complete(run);
                }else {
                    //Wait for future to be completed
                    try {
                        firstTaskWork.get();
                    } catch (Exception e) {
                        log.error("First task work failed", e);
                        fail(e.getMessage());
                    }
                }
            }

            @Override
            public void rejected(RejectedTaskReason reason) {
                rejections.incrementAndGet();
                CompletableFuture<Integer> work = executions.get(execution.getAndIncrement());
                work.complete(null);
                log.debug("Rejected: " + reason.getDescription() +  " @ " + simTimer.currentTimeMillis());
            }
        };

        //Schedule the task
        simTimer.schedule(task);

        //Advance simulation time to schedule all tasks
        long time = taskPeriod;
        for(int i=0; i<totalExecutions; i++) {
            simTimer.fastForwardTo(time);
            time+=taskPeriod;
        }

        //Allow first task to complete
        firstTaskWork.complete(null);

        //Wait for all executions
        try {
            CompletableFuture.allOf(executions.toArray(CompletableFuture[]::new)).get();
        } catch (Exception e) {
            log.error("Failed to complete", e);
        }

        Assert.assertEquals(totalExecutions, runs.get() + rejections.get());
        Assert.assertEquals(taskQueueSize + 1, runs.get());
        Assert.assertEquals(totalExecutions - (taskQueueSize + 1), rejections.get());

    }

    @Test(timeout = 60 * 1000 * 3)
    public void testSubmittingTasksNoQueueing() {

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

        long time = 5;
        for(int i=0; i<totalExecutions; i++) {
            CompletableFuture<Integer> work = new CompletableFuture<>();
            AtomicInteger result = new AtomicInteger(i);
            OneTimeTrigger trigger = new OneTimeTrigger(new Date(simTimer.currentTimeMillis()));
            TimerTask task = new TimerTask(trigger, "task1", "1", taskQueueSize) {

                @Override
                public void run(long runtime) {
                    runs.incrementAndGet();
                    work.complete(result.get());
                }

                @Override
                public void rejected(RejectedTaskReason reason) {
                    rejections.incrementAndGet();
                }
            };
            simTimer.schedule(task);
            simTimer.fastForwardTo(time);
            time+=100;

            //Wait for task to finish
            try {
                assertEquals(i, (int)work.get());
            } catch (Exception e) {
                log.error("Failed to get result", e);
                fail(e.getMessage());
            }
        }

        Assert.assertEquals(totalExecutions, runs.get());
        Assert.assertEquals(0, rejections.get());

    }

    @Test(timeout = 60 * 1000 * 3)
    public void testSubmittingTasksWithQueueing() {

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

        //Fill out the list of things to do
        CompletableFuture<Integer> firstTaskWork = new CompletableFuture<>();
        List<CompletableFuture> executions = new ArrayList<>();

        //Advance simulation time
        long time = 5;
        for(int i=0; i<totalExecutions; i++) {
            CompletableFuture<Integer> work;
            if(i > 0) {
                work = new CompletableFuture<>();
                executions.add(work);
            } else {
                work = firstTaskWork;
            }
            AtomicInteger result = new AtomicInteger(i);
            OneTimeTrigger trigger = new OneTimeTrigger(new Date(simTimer.currentTimeMillis()));
            TimerTask task = new TimerTask(trigger, "task1", "1", taskQueueSize) {

                @Override
                public void run(long runtime) {
                    runs.incrementAndGet();
                    if(result.get() > 0) {
                        work.complete(result.get());
                    }else {
                        //Wait for future to be completed for first task
                        try {
                            firstTaskWork.get();
                        } catch (Exception e) {
                            log.error("First task work failed", e);
                            fail(e.getMessage());
                        }
                    }
                }

                @Override
                public void rejected(RejectedTaskReason reason) {
                    rejections.incrementAndGet();
                    work.complete(null);
                }
            };
            simTimer.schedule(task);
            simTimer.fastForwardTo(time);
            time+=100;
        }

        //Allow first task to complete
        firstTaskWork.complete(null);

        //Wait for all executions
        try {
            CompletableFuture.allOf(executions.toArray(CompletableFuture[]::new)).get();
        } catch (Exception e) {
            log.error("Failed to complete", e);
        }

        Assert.assertEquals(totalExecutions, runs.get() + rejections.get());
        Assert.assertEquals(taskQueueSize + 1, runs.get());
        Assert.assertEquals(totalExecutions - (taskQueueSize + 1), rejections.get());

    }

    /**
     * Test multiple threads submitting the same task
     */
    @Test(timeout = 60 * 1000 * 3)
    public void testMultipleSubmitThreads() {

        int executionsPerThread = 100;
        int threads = 5;
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

        //Work for all threads
        List<CompletableFuture> threadWork = new ArrayList<>();
        CompletableFuture<Integer> firstTaskWork = new CompletableFuture<>();
        List<CompletableFuture> taskWork = new CopyOnWriteArrayList<>();

        //Submit one task to sit and block so queue can be filled
        Task task = new Task("task1", "1", queueSize) {
            @Override
            public void run(long runtime) {
                runs.incrementAndGet();
                taskOrder.add(taskOrderId.incrementAndGet());
                //Sit and wait for completion
                try {
                    firstTaskWork.get();
                } catch (Exception e) {
                    log.error("First task work failed", e);
                    fail(e.getMessage());
                }
            }

            @Override
            public void rejected(RejectedTaskReason reason) {
                rejections.incrementAndGet();
                fail("First task should not get rejected...");
            }
        };
        simTimer.execute(task);

        //Submit all other work
        for(int i=0; i<threads; i++) {
            CompletableFuture<Integer> tWork = new CompletableFuture<>();
            threadWork.add(tWork);
            new Thread() {
                @Override
                public void run() {
                    for(int j=0; j<executionsPerThread; j++) {
                        AtomicInteger result = new AtomicInteger(j);
                        CompletableFuture<Integer> work = new CompletableFuture<>();
                        taskWork.add(work);
                        Task task = new Task("task1", "1", queueSize) {
                            @Override
                            public void run(long runtime) {
                                runs.incrementAndGet();
                                taskOrder.add(taskOrderId.incrementAndGet());
                                work.complete(result.get());
                            }

                            @Override
                            public void rejected(RejectedTaskReason reason) {
                                rejections.incrementAndGet();
                                work.complete(null);
                            }
                        };
                        simTimer.execute(task);
                    }
                    tWork.complete(null);
                };
            }.start();
        }

        //Wait for all thread work to finish submitting tasks
        try {
            CompletableFuture.allOf(threadWork.toArray(CompletableFuture[]::new)).get();
        } catch (Exception e) {
            log.error("Failed to complete all thread work", e);
        }

        //Finish first task's work
        firstTaskWork.complete(null);

        //Ensure all task work is finished
        try {
            CompletableFuture.allOf(taskWork.toArray(CompletableFuture[]::new)).get();
        } catch (Exception e) {
            log.error("Failed to complete all task work", e);
        }

        //Ensure number of executions
        Assert.assertEquals(6, runs.get());
        Assert.assertEquals(executionsPerThread * threads - queueSize, rejections.get());

        //Ensure order for task execution
        Assert.assertEquals(1, (int)taskOrder.get(0));
        Assert.assertEquals(2, (int)taskOrder.get(1));
        Assert.assertEquals(3, (int)taskOrder.get(2));
        Assert.assertEquals(4, (int)taskOrder.get(3));
        Assert.assertEquals(5, (int)taskOrder.get(4));
        Assert.assertEquals(6, (int)taskOrder.get(5));
    }

    /*
     * Simulate a timeout task getting rejected and then throwing an exception from
     *   within the rejected callback
     */
    @Test(timeout = 60 * 1000 * 3)
    public void testTimerThreadRejectionExceptions() throws InterruptedException {
        IgnoreExpectedException handler = new IgnoreExpectedException();
        OrderedRealTimeTimer timer = new OrderedRealTimeTimer();
        OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
                0,
                3,
                30L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new MangoThreadFactory("high", Thread.MAX_PRIORITY - 2, Thread.currentThread().getContextClassLoader(), handler),
                false,
                timer.getTimeSource());
        timer.init(executor, Thread.MAX_PRIORITY);

        final AtomicLong time = new AtomicLong();
        final AtomicInteger runs = new AtomicInteger();

        List<CompletableFuture> waitingOn = new ArrayList<>();
        List<CompletableFuture> executions = new ArrayList<>();

        for(int i=0; i<3; i++) {
            CompletableFuture<Integer> work = new CompletableFuture<>();
            waitingOn.add(work);
            CompletableFuture<Integer> run = new CompletableFuture<>();
            executions.add(run);
            AtomicInteger result = new AtomicInteger(i);
            Task task = new Task("TaskName", "TaskId" + i, 0) {

                @Override
                public void run(long runtime) {
                    //Sit and wait for completion
                    try {
                        runs.getAndIncrement();
                        work.get();
                        run.complete(result.get());
                    } catch (Exception e) {
                        log.error("Work failed", e);
                        fail(e.getMessage());
                    }
                }

                @Override
                public void rejected(RejectedTaskReason reason) {
                    fail(reason.getDescription());
                }
            };
            TaskWrapper wrapper = new TaskWrapper(task, time.getAndAdd(100));
            executor.execute(wrapper);
        }

        CompletableFuture<Integer> rejectedWork = new CompletableFuture<>();
        //Execute the timeout task to be rejected
        new com.serotonin.m2m2.util.timeout.TimeoutTask(
                new OneTimeTrigger(0), new TimeoutClient() {

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
                        rejectedWork.complete(null);
                        //Throw exception here and ensure it doesn't kill the timer thread
                        throw new ExpectedException();
                    }
                }, timer);

        //Ensure the exception is thrown
        try {
            rejectedWork.get();
        } catch (Exception e) {
            log.error("Failed to reject task", e);
        }

        //Let the tasks run through
        for(CompletableFuture<Integer> w : waitingOn) {
            w.complete(null);
        }

        //Wait for all runs to complete
        try {
            CompletableFuture.allOf(executions.toArray(CompletableFuture[]::new)).get();
        } catch (Exception e) {
            log.error("Failed to complete all task work", e);
        }

        //Ensure the tasks all ran 3 times
        Assert.assertEquals(3, runs.get());

        //This will be set to false if the Timer Thread Fails
        Assert.assertEquals(timer.thread.newTasksMayBeScheduled, true);
        handler.throwIfNotEmpty();
    }

}
