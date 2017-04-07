/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer;

/**
 * This class is useful to monitor the Tasks inside on an Ordered Timer
 * 
 * @author Terry Packer
 *
 */
public class OrderedTaskInfo {

	/**
	 * Id of task
	 */
	String id;	
	
	/**
	 * Name of task
	 */
	String name;
	
	/**
	 *  Limit of the queue size
	 */
	int queueSizeLimit = 0;
	
	/* Runtime Members */
	/**
	 * Maximum size the queue has ever been
	 */
	int maxQueueSize = 0;
	
	
	/**
	 * Current Queue Size
	 */
	int currentQueueSize = 0;
	
	/**
	 * Number of times tasks have been executed from the queue
	 */
	long executionCount = 0l;
	
	/**
	 * Average execution time
	 */
	long avgExecutionTimeMs = 0l;
	
	/**
	 * Time of the last execution
	 */
	long lastExecutionTimeMs = 0l;
	
	/**
	 * Total rejections from queue
	 */
	int rejections = 0;

	public OrderedTaskInfo(){ }
	
	/**
	 * @param task
	 */
	public OrderedTaskInfo(Task task) {
		this.id = task.id;
		this.name = task.name;
		this.queueSizeLimit = task.queueSize;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMaxQueueSize() {
		return maxQueueSize;
	}

	public int getCurrentQueueSize() {
		return currentQueueSize;
	}

	public long getExecutionCount() {
		return executionCount;
	}

	public long getAvgExecutionTimeMs() {
		return avgExecutionTimeMs;
	}
	
	public long getLastExecutionTimeMs(){
		return this.lastExecutionTimeMs;
	}
	
	public int getQueueSizeLimit(){
		return this.queueSizeLimit;
	}
	
	public int getRejections(){
		return rejections;
	}
	
	/**
	 * Add one execution time to our average and increase
	 * the run count by 1
	 * @param ms
	 */
	public void addExecutionTime(long ms){
		this.executionCount++;
		this.avgExecutionTimeMs = this.avgExecutionTimeMs * (this.executionCount - 1l)/this.executionCount + ms/this.executionCount;
		this.lastExecutionTimeMs = ms;
	}
	
	/**
	 * Update the current size and track the max
	 * @param size
	 */
	public void updateCurrentQueueSize(int size){
		if(size > this.maxQueueSize)
			this.maxQueueSize = size;
		this.currentQueueSize = size;
	}
	
}
