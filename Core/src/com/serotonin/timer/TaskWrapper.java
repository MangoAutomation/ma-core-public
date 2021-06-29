/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

/**
 * Wrapper for an instance of the Task run
 * @author Terry Packer
 *
 */
public class TaskWrapper implements Runnable{
	Task task;
	long executionTime;
	
	public TaskWrapper(Task task, long executionTime){
		this.task = task;
		this.executionTime = executionTime;
	}
	
	@Override
	final public void run(){
		task.runTask(executionTime);
	}
	
    public long getExecutionTime(){
    	return this.executionTime;
    }

	public Task getTask() {
		return task;
	}
	
	public boolean cancel(){
		return this.task.cancel();
	}
}
