/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.maint;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Terry Packer
 *
 */
public class MangoThreadFactory implements ThreadFactory{

	private String prefix;
	private ThreadFactory factory;
	private int priority;
	
	/**
	 * 
	 * @param namePrefix
	 * @param threadPriority
	 */
	public MangoThreadFactory(String namePrefix, int threadPriority){
		this.prefix = namePrefix + "-";
		this.priority = threadPriority;
		this.factory = Executors.defaultThreadFactory();
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = factory.newThread(r);
		String name = t.getName();
		//For now simply add our priority to the front of the name
		// we could get fancier but this will suffice for now.
		// the thread name will look like this medium-pool-x-thread-x
		name = prefix + name;
		t.setName(name);
		t.setPriority(priority);
		return t;
	}
	

}
