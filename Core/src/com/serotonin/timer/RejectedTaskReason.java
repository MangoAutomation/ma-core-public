/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import java.util.concurrent.Executor;

/**
 * Wrapper class to allow information about rejected tasks as there are additional reasons 
 * tasks can be rejected from the custom Executors in this library
 * 
 * @author Terry Packer
 *
 */
public class RejectedTaskReason {

	public static final int POOL_FULL = 1; //The thread pool's workers are all busy
	public static final int TASK_QUEUE_FULL = 2; //The Executor's queue for this task is at capacity
	public static final int CURRENTLY_RUNNING = 3; //The task is Ordered and one of its worker instances is already running
	
	private int code;
	private long scheduledExecutionTime;
	private Task task;
	private Executor executor;
	
	/**
	 *
     */
	public RejectedTaskReason(int reasonCode, long scheduledExecutionTime, Task task, Executor e){
		this.code = reasonCode;
		this.scheduledExecutionTime = scheduledExecutionTime;
		this.task = task;
		this.executor = e;
	}
	
	public int getCode(){
		return code;
	}
	
	public String getDescription(){
		switch(code){
		case POOL_FULL:
			return "Pool Full";
		case TASK_QUEUE_FULL:
			return "Task Queue Full";
		case CURRENTLY_RUNNING:
			return "Task Currently Running";
		default:
			return "Unknown";
		}
	}
	
	/**
	 * Get the time at which the task should have fired
     */
	public long getScheduledExecutionTime(){
		return this.scheduledExecutionTime;
	}
	
	/**
	 *
     */
	public Task getTask(){
		return this.task;
	}
	
	/**
	 * Get the executor that it was rejected from
     */
	public Executor getExecutor(){
		return this.executor;
	}
	
}
