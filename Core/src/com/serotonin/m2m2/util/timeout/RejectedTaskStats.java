/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.RejectedTaskReason;

/**
 * @author Terry Packer
 *
 */
public 	class RejectedTaskStats {
	
	private String id;
	private String name;
	/* Time last added info */
	private long lastAccess;
	
	/* Rejection counts */
	private long totalRejections;
	private long currentlyRunning;
	private long poolFull;
	private long queueFull;
	
	/* Period to delay logging */
	private int logPeriod;
	
	
	public RejectedTaskStats(String id, String name, int logPeriod){
		this.id = id;
		this.name = name;
		this.logPeriod = logPeriod;
	}
	
	/**
	 * Update our stats and inform if this is a loggable event
	 * @param reason
	 * @return
	 */
	public boolean update(RejectedTaskReason reason){
		long now = Common.timer.currentTimeMillis();
		totalRejections++;
		
		//Track individual reasons
		switch(reason.getCode()){
		case RejectedTaskReason.CURRENTLY_RUNNING:
			currentlyRunning++;
			break;
		case RejectedTaskReason.POOL_FULL:
			poolFull++;
			break;
		case RejectedTaskReason.TASK_QUEUE_FULL:
			queueFull++;
			break;
		}
		
		//Track logging
		if( now  > (lastAccess + logPeriod)){
			lastAccess = now;
			return true;
		}else
			return false;
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

	public long getLastAccess() {
		return lastAccess;
	}

	public void setLastAccess(long lastAccess) {
		this.lastAccess = lastAccess;
	}

	public long getTotalRejections() {
		return totalRejections;
	}

	public void setTotalRejections(long totalRejections) {
		this.totalRejections = totalRejections;
	}

	public long getPoolFull() {
		return poolFull;
	}

	public void setPoolFull(long poolFull) {
		this.poolFull = poolFull;
	}

	public long getQueueFull() {
		return queueFull;
	}

	public void setQueueFull(long queueFull) {
		this.queueFull = queueFull;
	}

	public long getCurrentlyRunning() {
		return currentlyRunning;
	}

	public void setCurrentlyRunning(long currentlyRunning) {
		this.currentlyRunning = currentlyRunning;
	}		
}