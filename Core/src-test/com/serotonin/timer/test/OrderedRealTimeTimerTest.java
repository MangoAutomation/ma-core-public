/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer.test;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.OrderedRealTimeTimer;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;

/**
 * @author Terry Packer
 *
 */
public class OrderedRealTimeTimerTest {

	public static void main(String[] args) {
		
		OrderedRealTimeTimer timer = new OrderedRealTimeTimer();
		ThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
        		0, 
        		100, 
        		30L, 
        		TimeUnit.SECONDS, 
        		new SynchronousQueue<Runnable>(), 
        		false);
		
		timer.init(executor);
		
		FixedRateTrigger trigger = new FixedRateTrigger(0, 5);
		TimerTask task = new TimerTask(trigger, "task1", "1", 5){
			
			private int count = 0;

			@Override
			public void run(long runtime) {
				count++;

				System.out.println("Run: " + count);
				System.out.println("Run at:" + runtime);
				System.out.println("");
				try {
					Thread.sleep(4);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void rejected(RejectedTaskReason reason) {
				System.out.println("Rejected");
			}
		};

		try {
			Thread.sleep(100);
		
			timer.schedule(task);
		

			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
