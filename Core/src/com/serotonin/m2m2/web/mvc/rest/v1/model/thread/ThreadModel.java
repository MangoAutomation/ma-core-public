/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.thread;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * @author Terry Packer
 *
 */
public class ThreadModel {
	
	private ThreadInfo info;
	private Thread thread;
	private long cpuTime;
	private long userTime;

	
	public ThreadModel(ThreadInfo info, Thread thread, long cpuTime, long userTime){
		this.info = info;
		this.thread = thread;
		this.cpuTime = cpuTime;
		this.userTime = userTime;
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
		return this.cpuTime;
	}
	
	@JsonGetter("userTime")
	public long getUserTime(){
		return this.userTime;
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
