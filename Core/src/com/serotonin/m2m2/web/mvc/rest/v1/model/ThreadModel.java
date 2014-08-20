/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * @author Terry Packer
 *
 */
public class ThreadModel {
	
	private ThreadInfo info;
	private Thread thread;
	
	public ThreadModel(){
		
	}
	
	public ThreadModel(ThreadInfo info, Thread thread){
		this.info = info;
		this.thread = thread;
	}

	
	@JsonGetter("id")
	public long getId(){
		return this.info.getThreadId();
	}
	
	
	@JsonGetter("name")
	public String getName(){
		return this.info.getThreadName();
	}

	@JsonGetter("cpuTime")
	public long getCpuTime(){
		return 0;
	}

	@JsonGetter("state")
	public State getState(){
		return this.info.getThreadState();
	}
	
	@JsonGetter("priority")
	public int getPriority(){
		return this.thread.getPriority();
	}

	@JsonGetter("location")
	public StackTraceElement[] getLocation(){
		return this.info.getStackTrace();
	}

}
