/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.timeout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.OrderedThreadPoolExecutor.OrderedTaskCollection;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TaskWrapper;
import com.serotonin.timer.TimerTask;

/**
 * Class to handle rejected tasks from the 3 thread pools, keeping some statistics and 
 * preventing too much logging from happening.
 * 
 * 
 * @author Terry Packer
 *
 */
public class TaskRejectionHandler extends TimerTask implements RejectedExecutionHandler{
	
	final Logger log = LoggerFactory.getLogger(TaskRejectionHandler.class);
	
	/* Period after which Task Rejection Stats become removeable */
	private long staleTaskStatsPeriod = 100000000;
	
	/* Period to wait before logging another rejection for a given task */
	private int logPeriod;
	
	/* Map of rejected tasks and their stats */
	private final Map<String, RejectedTaskStats> statsMap;
	
	private final Queue<RejectedTaskStats> unOrderedRejections;
	private long lastUnorderedRejection;
	
	/**
	 * Create the task rejection handler
	 */
	public TaskRejectionHandler(){
		super(new FixedRateTrigger(0, 10000), "TaskRejectionHandler cleaner");
		this.logPeriod = Common.envProps.getInt("runtime.taskRejectionLogPeriod", 10000);
		this.statsMap = new ConcurrentHashMap<String, RejectedTaskStats>();
		this.unOrderedRejections = new ConcurrentLinkedQueue<>();
	}

	/**
	 * Task was rejected, track its statistics and provide logging
	 * @param reason
	 */
	public void rejectedTask(RejectedTaskReason reason){
		
		String id = reason.getTask().getId();
		if(id != null){
			RejectedTaskStats stats = this.statsMap.computeIfAbsent(id, (k) -> {
				return new RejectedTaskStats(id, reason.getTask().getName(), this.logPeriod);
			});
	
			//Is it time to
			if(log.isDebugEnabled() && stats.update(reason))
				log.debug("Rejected task: " + reason.getTask().getName() + " because " + reason.getDescription());
		}else{
			//Task without an ID was rejected
			long now = Common.timer.currentTimeMillis();
			if(log.isDebugEnabled() && (now  > (lastUnorderedRejection + logPeriod))){
				lastUnorderedRejection = now;
				log.debug("Rejected task: " + reason.getTask().getName() + " because " + reason.getDescription());
			}
			//TODO if we want we could track these by Name here since the name probably is unique but this 
			// would take processor time from the CPU and is not necessary
			RejectedTaskStats stats = new RejectedTaskStats(null, reason.getTask().getName(), this.logPeriod);
			stats.update(reason);
			this.unOrderedRejections.add(stats);
		}
	}
	
	/*
	 * This will be called by the Executor when Runnable's are not executable due to pool constraints
	 * (non-Javadoc)
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
		if(r instanceof TaskWrapper){
			TaskWrapper wrapper = (TaskWrapper)r;
			RejectedTaskReason reason = new RejectedTaskReason(RejectedTaskReason.POOL_FULL, wrapper.getExecutionTime(), wrapper.getTask(), e);
			wrapper.getTask().rejectedAsDelegate(reason);
			this.rejectedTask(reason);
		}else if (r instanceof OrderedTaskCollection){
			//Pool must be full since the entire collection was rejected
			TaskWrapper wrapper = ((OrderedTaskCollection)r).getWrapper();
			RejectedTaskReason reason = new RejectedTaskReason(RejectedTaskReason.POOL_FULL, wrapper.getExecutionTime(), wrapper.getTask(), e);
			wrapper.getTask().rejectedAsDelegate(reason);
			this.rejectedTask(reason);
		}else{
			log.error("SHOULD NOT HAPPEN: " + r.toString());
		}
	}
	
	/**
	 * Get a list of the current rejection stats
	 * @return
	 */
	public List<RejectedTaskStats> getRejectedTaskStats(){
		List<RejectedTaskStats> all = new ArrayList<RejectedTaskStats>(this.statsMap.size());
		Iterator<String> it = this.statsMap.keySet().iterator();
		while(it.hasNext())
			all.add(this.statsMap.get(it.next()));
		
		//Now add in all the tasks without IDs
		Iterator<RejectedTaskStats> noIds = this.unOrderedRejections.iterator();
		while(noIds.hasNext())
			all.add(noIds.next());
		return all;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run(long runtime) {
		long now = Common.timer.currentTimeMillis();
		Iterator<String> it = this.statsMap.keySet().iterator();
		while(it.hasNext()){
			RejectedTaskStats stats = this.statsMap.get(it.next());
			if(now > stats.getLastAccess() + this.staleTaskStatsPeriod)
				it.remove();
		}

		Iterator<RejectedTaskStats> uIt = this.unOrderedRejections.iterator();
		while(uIt.hasNext()){
			RejectedTaskStats stats = uIt.next();
			if(now > stats.getLastAccess() + this.staleTaskStatsPeriod)
				it.remove();
		}
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		this.rejectedTask(reason);
	}
	
}
