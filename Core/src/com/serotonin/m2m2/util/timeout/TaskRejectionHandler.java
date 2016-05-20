/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.RejectedTaskReason;
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
	
	final Log log = LogFactory.getLog(TaskRejectionHandler.class);
	
	/* Period after which Task Rejection Stats become removeable */
	private long staleTaskStatsPeriod = 100000000;
	
	/* Period to wait before logging another rejection for a given task */
	private int logPeriod = 1000;
	
	/* Map of rejected tasks and thier stats */
	private final Map<String, RejectedTaskStats> highPriorityStatsMap;
	
	/**
	 * TODO Do we want a task ID?
	 * Create the task rejection handler
	 */
	public TaskRejectionHandler(){
		super(new FixedRateTrigger(0, 10000), "TaskRejectionHandler cleaner");
		this.highPriorityStatsMap = new ConcurrentHashMap<String, RejectedTaskStats>();
	}

	/**
	 * Task was rejected, track its statistics and provide logging
	 * @param reason
	 */
	public void rejectedHighPriorityTask(RejectedTaskReason reason){
		
		String id = reason.getTask().getId();

		RejectedTaskStats stats = this.highPriorityStatsMap.get(id);
		if(stats == null){
			stats = new RejectedTaskStats(id, reason.getTask().getName(), this.logPeriod);
			this.highPriorityStatsMap.put(id, stats);
		}

		//Is it time to
		if(stats.update(reason) && log.isDebugEnabled())
			log.debug("Rejected task: " + reason.getTask().getName() + " because " + reason.getDescription());
	}
	
	/*
	 * This will be called by the Executor when Runnable's are not executable due to pool constraints
	 * this shouldn't happen in the high priority pool.
	 * (non-Javadoc)
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
		log.fatal("SHOULD NOT HAPPEN: " + r.toString());
//		//TODO Types of WorkItem, Runnable and ScheduledRunnable can also be rejected here so we must handle those too for now
//		if(r instanceof TaskWrapper){
//			TaskWrapper wrapper = (TaskWrapper)r;
//			wrapper.getTask().rejected(new RejectedTaskReason(RejectedTaskReason.POOL_FULL, wrapper.getExecutionTime(), wrapper.getTask().getName(), wrapper.getTask(), e));
//		}else{
//			LOG.debug("Task rejected: " + r.toString());
//		}
	}
	
	/**
	 * Get a list of the current rejection stats
	 * @return
	 */
	public List<RejectedTaskStats> getRejectedHighPriorityTaskStats(){
		List<RejectedTaskStats> all = new ArrayList<RejectedTaskStats>(this.highPriorityStatsMap.size());
		Iterator<String> it = this.highPriorityStatsMap.keySet().iterator();
		while(it.hasNext())
			all.add(this.highPriorityStatsMap.get(it.next()));
		
		return all;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run(long runtime) {
		long now = Common.backgroundProcessing.currentTimeMillis();
		Iterator<String> it = this.highPriorityStatsMap.keySet().iterator();
		while(it.hasNext()){
			RejectedTaskStats stats = this.highPriorityStatsMap.get(it.next());
			if(now > stats.getLastAccess() + this.staleTaskStatsPeriod)
				it.remove();
		}
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		//TODO We have processor time, maybe clean here?
		this.rejectedHighPriorityTask(reason);
	}
	
}
