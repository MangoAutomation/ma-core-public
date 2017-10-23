/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2;

import java.io.File;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.serotonin.m2m2.db.AbstractDatabaseProxy;
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
	
	protected void configureH2Proxy(File baseTestDir){
		Common.envProps.setDefaultValue("db.url", "jdbc:h2:" + baseTestDir.getAbsolutePath() + File.separator + "h2");
		Common.envProps.setDefaultValue("db.location", baseTestDir.getAbsolutePath() + File.separator + "h2");
		Common.envProps.setDefaultValue("db.nosql.location", baseTestDir.getAbsolutePath());
		Common.envProps.setDefaultValue("db.username", "");
		Common.envProps.setDefaultValue("db.password", "");
		
		Common.databaseProxy = AbstractDatabaseProxy.createDatabaseProxy();
		Common.databaseProxy.initialize(ClassLoader.getSystemClassLoader());
	}
	
	protected void configureMySqlProxy(String host, int port, String databaseName) {
		Common.envProps.setDefaultValue("db.type", "mysql");
		Common.envProps.setDefaultValue("db.url", "jdbc:mysql://"+host+(port != 3306 ? ":"+port : "") + "/" + databaseName);
		Common.envProps.setDefaultValue("db.username", "mango");
		Common.envProps.setDefaultValue("db.password", "mango");
		
		Common.databaseProxy = AbstractDatabaseProxy.createDatabaseProxy();
		Common.databaseProxy.initialize(ClassLoader.getSystemClassLoader());
	}
	
	
}
