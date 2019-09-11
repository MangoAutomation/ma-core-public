/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer;

import java.util.Date;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Terry Packer
 *
 */
public class OrderedRealTimeTimerTest {

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
	
}
