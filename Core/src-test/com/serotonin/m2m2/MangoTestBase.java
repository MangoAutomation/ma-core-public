/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import java.io.File;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.serotonin.m2m2.db.DatabaseProxy;
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
        		new RejectedRunnableEventGenerator(),
        		Common.envProps.getInt("runtime.realTimeTimer.defaultTaskQueueSize", 1),
        		Common.envProps.getBoolean("runtime.realTimeTimer.flushTaskQueueOnReject", false));
        Common.timer.init(executor);
		
		
	}
	
	protected void configureH2Proxy(File baseTestDir){
		Common.envProps.setDefaultValue("db.url", "jdbc:h2:" + baseTestDir.getAbsolutePath() + File.separator + "h2");
		Common.envProps.setDefaultValue("db.location", baseTestDir.getAbsolutePath() + File.separator + "h2");
		Common.envProps.setDefaultValue("db.nosql.location", baseTestDir.getAbsolutePath());
		
		Common.databaseProxy = DatabaseProxy.createDatabaseProxy();
		Common.databaseProxy.initialize(ClassLoader.getSystemClassLoader());
	}
	
	
}
