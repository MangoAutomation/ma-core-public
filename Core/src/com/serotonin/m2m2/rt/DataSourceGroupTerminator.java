/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.TimerTask;

/**
 * This class is used at startup to initialize data sources in parallel.
 * 
 * The group is generally a list of all data sources with the same priority level.
 * The group is not initalized until all data sources have either started or failed to start.
 * 
 * @author Terry Packer
 *
 */
public class DataSourceGroupTerminator {
	private final Log LOG = LogFactory.getLog(DataSourceGroupTerminator.class);
			
	private List<DataSourceRT> group;
	private int threadPoolSize;
	private List<DataSourceSubGroupTerminator> runningTasks;
	
	/**
	 * @param priorityList
	 */
	public DataSourceGroupTerminator(List<DataSourceRT> group, int threadPoolSize) {
		this.group = group;
		this.threadPoolSize = threadPoolSize;
	}

	/**
	 * Blocking method that will attempt to start all datasources in parallel using threadPoolSize number of threads at most.
	 * @return List of all data sources that need to begin polling.
	 */
	public void terminate() {
		
		if(this.group == null)
			return;
		
		//Compute the size of the subGroup that each thread will use.
		int subGroupSize = this.group.size() / this.threadPoolSize;
		
		LOG.info("Starting " + this.group.size() + " data sources in " + this.threadPoolSize + " threads.");
		
		this.runningTasks = new ArrayList<DataSourceSubGroupTerminator>(this.threadPoolSize);
		//Add and Start the tasks 
		int endPos;
		for(int i=0; i<this.threadPoolSize; i++){
			if(i==this.threadPoolSize-1){
				//Last group may be larger
				endPos = this.group.size();
			}else{
				endPos = (i*subGroupSize) + subGroupSize;
			}
			DataSourceSubGroupTerminator currentSubgroup = new DataSourceSubGroupTerminator(this.group.subList(i*subGroupSize, endPos), this);
			
			synchronized(this.runningTasks){
				this.runningTasks.add(currentSubgroup);
			}
			Common.timer.execute(currentSubgroup);
		}
		
		//Wait here until all threads are finished
		while(runningTasks.size() > 0){
			try { Thread.sleep(100); } catch (InterruptedException e) { }
		}
		
		return;
	}

	/**
	 * Remove a running task from our list, when empty the terminator is finished
	 * @param task
	 */
	public void removeRunningTask(DataSourceSubGroupTerminator task){
		synchronized(this.runningTasks){
			this.runningTasks.remove(task);
		}
	}
	
	/**
	 * Initialize a sub group of the data sources in one thread.
	 * @author Terry Packer
	 *
	 */
	class DataSourceSubGroupTerminator extends TimerTask{
		
		private final Log LOG = LogFactory.getLog(DataSourceSubGroupTerminator.class);
		
		private List<DataSourceRT> subgroup;
		private DataSourceGroupTerminator parent;
		
		public DataSourceSubGroupTerminator(List<DataSourceRT> subgroup, DataSourceGroupTerminator parent){
			super(new OneTimeTrigger(0));
			this.subgroup = subgroup;
			this.parent = parent;
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.TimerTask#run(long)
		 */
		@Override
		public void run(long runtime) {
			try{
				for(DataSourceRT config : subgroup)
					Common.runtimeManager.stopDataSourceShutdown(config.getId());
			}catch(Exception e){
				LOG.error(e.getMessage(), e);
			}finally{
				this.parent.removeRunningTask(this);
			}
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.timer.TimerTask#cancel()
		 */
		@Override
		public boolean cancel() {
			this.parent.removeRunningTask(this);
			return super.cancel();
		}
	}
	
}
