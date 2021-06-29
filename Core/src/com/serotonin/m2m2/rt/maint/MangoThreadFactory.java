/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.maint;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Thread factory to set a priority and the module's classloader to ensure
 * one has access to the module defined classes during execution. 
 * 
 * @author Terry Packer
 *
 */
public class MangoThreadFactory implements ThreadFactory {

	private final String prefix;
	private final ThreadFactory factory;
	private final int priority;
	private final ClassLoader contextClassLoader;
	
	/**
	 * 
	 * @param namePrefix
	 * @param threadPriority
	 */
	public MangoThreadFactory(String namePrefix, int threadPriority, ClassLoader moduleClassLoader){
		this.prefix = namePrefix + "-";
		this.priority = threadPriority;
		this.factory = Executors.defaultThreadFactory();
		this.contextClassLoader = moduleClassLoader;
	}

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
		t.setContextClassLoader(contextClassLoader);
		return t;
	}
	

}
