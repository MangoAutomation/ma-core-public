/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.OrderedRealTimeTimer;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.RealTimeTimer;
import com.serotonin.util.properties.ReloadingProperties;

/**
 * @author Terry Packer
 *
 */
public class MangoTestBase {

	/**
	 * This requires ma-home to be set on the path
	 */
	protected void initMaHome(){
		Common.MA_HOME = System.getProperty("ma.home");
	}
	
	protected void initEnvProperties(String envPropertiesName){
		Common.envProps = new ReloadingProperties(envPropertiesName);
	}
	
	protected void initTimer(){
		
		ThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
        		0, 
        		100, 
        		30L, 
        		TimeUnit.SECONDS, 
        		new SynchronousQueue<Runnable>(), 
        		new MangoThreadFactory("high", Thread.MAX_PRIORITY),
        		new RejectedExecutionHandler() {
					
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						System.out.println("Rejected task: " + r);
					}
				},
        		Common.envProps.getBoolean("runtime.realTimeTimer.flushTaskQueueOnReject", false));
        
        final RealTimeTimer timer = new OrderedRealTimeTimer();
        timer.init(executor);
        Providers.add(TimerProvider.class, new TimerProvider<RealTimeTimer>() {
            @Override
            public RealTimeTimer getTimer() {
                return timer;
            }
        });		
	}
	
	
}
