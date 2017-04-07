/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.infiniteautomation.mango.monitor.IntegerMonitor;
import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.OrderedRealTimeTimer;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.RealTimeTimer;

/**
 * 
 * @author Terry Packer
 */
public class MonitoredValuesTest {
	
	static final MonitoredValues MONITORED_VALUES = new MonitoredValues();
	static final String MONITOR_ID = "monitor";
	final int period = 100; //Ms period
	
	
	public void loadTest(){
		
		//Setup a Timer
		RealTimeTimer timer = new OrderedRealTimeTimer();
		ThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
        		0, 
        		100, 
        		30L, 
        		TimeUnit.SECONDS, 
        		new SynchronousQueue<Runnable>(), 
        		new MangoThreadFactory("high", Thread.MAX_PRIORITY),
        		new RejectedExecutionHandler(){

					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						System.out.println("Rejected: " + r.toString());
					}
        		},
        		false);
        timer.init(executor);
		
		//Create a monitor
        IntegerMonitor monitor = new IntegerMonitor(MONITOR_ID, new TranslatableMessage("internal.monitor.BATCH_ENTRIES"), new ValueMonitorOwner(){

			@Override
			public void reset(String monitorId) {
				IntegerMonitor mon = (IntegerMonitor) MONITORED_VALUES.getValueMonitor(MONITOR_ID);
				mon.reset();
			}
        	
        });
		MONITORED_VALUES.addIfMissingStatMonitor(monitor);
        
		//Start a task to count up
		new TimeoutTask(new FixedRateTrigger(0, period), new TimeoutClient(){
			@Override
			public void scheduleTimeout(long fireTime) {
				IntegerMonitor mon = (IntegerMonitor) MONITORED_VALUES.getValueMonitor(MONITOR_ID);
				mon.addValue(1);
			}}, timer );
		
		//Start a task to count down
		new TimeoutTask(new FixedRateTrigger(0, period), new TimeoutClient(){
			@Override
			public void scheduleTimeout(long fireTime) {
				IntegerMonitor mon = (IntegerMonitor) MONITORED_VALUES.getValueMonitor(MONITOR_ID);
				mon.addValue(-1);
			}}, timer );
		
		//Start a task to read
		new TimeoutTask(new FixedRateTrigger(0, period), new TimeoutClient(){
			@Override
			public void scheduleTimeout(long fireTime) {
				IntegerMonitor mon = (IntegerMonitor) MONITORED_VALUES.getValueMonitor(MONITOR_ID);
				mon.getValue();
			}}, timer );
		//Start a task to reset
		new TimeoutTask(new FixedRateTrigger(0, period), new TimeoutClient(){
			@Override
			public void scheduleTimeout(long fireTime) {
				IntegerMonitor mon = (IntegerMonitor) MONITORED_VALUES.getValueMonitor(MONITOR_ID);
				mon.reset();
			}}, timer );
		
		while(true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
