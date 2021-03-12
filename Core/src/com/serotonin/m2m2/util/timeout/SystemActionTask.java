/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.util.timeout;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.timer.TimerTask;
import com.serotonin.timer.TimerTrigger;

/**
 * Wrapper for system actions.  Place any data for progressive status updates etc into 'results'
 * which can be periodically polled via the REST endpoint.
 * 
 * @author Terry Packer
 */
public abstract class SystemActionTask extends TimerTask {

	public static final int PRIORITY_HIGH = WorkItem.PRIORITY_HIGH;
	public static final int PRIORITY_MEDIUM = WorkItem.PRIORITY_MEDIUM;
	
	protected static final Log LOG = LogFactory.getLog(SystemActionTask.class);
	
	/**
	 * Thread safe results for progressive tracking
	 */
	protected ConcurrentHashMap<String, Object> results;
	protected boolean finished = false;
	
	/**
	 * Unqueued task
	 * @param trigger
	 * @param name
	 */
	public SystemActionTask(TimerTrigger trigger, String name) {
		super(trigger, name);
		this.results = new ConcurrentHashMap<String, Object>();
	}

	/**
	 * Create an ordered queueable task
	 * @param trigger
	 * @param name
	 * @param id
	 * @param queueSize
	 */
	public SystemActionTask(TimerTrigger trigger, String name, String id, int queueSize){
		super(trigger, name, id, queueSize);
		this.results = new ConcurrentHashMap<String, Object>();
		this.results.put("finished", false);
	}
	
	public ConcurrentHashMap<String, Object> getResults(){
		return results;
	}
	
	/**
	 * Perform any work here, exceptions are caught and logged
	 * @param runtime
	 */
	protected abstract void runImpl(long runtime);
	
	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#run(long)
	 */
	@Override
	public void run(long runtime) {
		try{
			this.runImpl(runtime);
		}catch(Exception e){
			LOG.error(e.getMessage(), e);
			this.results.put("exception", e.getMessage());
			this.results.put("failed", true);
		}finally{
			this.finished = true;
			this.results.put("finished", true);
			this.results.put("cancelled", this.isCancelled());
			this.results.put("completeBeforeCancel", this.isCompleteBeforeCancel());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#cancel()
	 */
	@Override
	public boolean cancel() {
		boolean result = super.cancel();
		this.results.put("cancelled", true);
		return result;
	}
	
	public boolean isFinished(){
		return this.finished;
	}
	
	/**
	 * Get the task priority
	 * @return
	 */
	public int getPriority(){
		return PRIORITY_MEDIUM;
	}
}
