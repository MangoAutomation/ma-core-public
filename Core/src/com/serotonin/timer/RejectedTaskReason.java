/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
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
	 * @param reasonCode
	 * @param scheduledExecutionTime
	 * @param task
	 * @param e
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
	 * @return
	 */
	public long getScheduledExecutionTime(){
		return this.scheduledExecutionTime;
	}
	
	/**
	 * 
	 * @return
	 */
	public Task getTask(){
		return this.task;
	}
	
	/**
	 * Get the executor that it was rejected from
	 * @return
	 */
	public Executor getExecutor(){
		return this.executor;
	}
	
}
