/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.m2m2.util.timeout.RejectedRunnableEventGenerator;
import com.serotonin.timer.OrderedThreadPoolExecutor;
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
        		Common.envProps.getInt("runtime.realTimeTimer.defaultTaskQueueSize", 1),
        		Common.envProps.getBoolean("runtime.realTimeTimer.flushTaskQueueOnReject", false));
        executor.setRejectedExecutionHandler(new RejectedRunnableEventGenerator());
        Common.timer.init(executor);
		
		
	}
	
	
}
