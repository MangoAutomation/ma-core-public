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
import com.serotonin.m2m2.module.DataSourceDefinition.StartPriority;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
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
public class DataSourceGroupInitializer {
	private final Log LOG = LogFactory.getLog(DataSourceGroupInitializer.class);
			
	private List<DataSourceVO<?>> group;
	private int threadPoolSize;
	private List<DataSourceSubGroupInitializer> runningTasks;
	private List<DataSourceVO<?>> polling;
	private boolean useMetrics;
	private StartPriority startPriority;

	/**
	 * 
	 * @param group
	 * @param logMetrics
	 * @param threadPoolSize
	 */
	public DataSourceGroupInitializer(StartPriority startPriority, List<DataSourceVO<?>> group, boolean logMetrics, int threadPoolSize) {
		this.startPriority = startPriority;;
		this.group = group;
		this.useMetrics = logMetrics;
		this.threadPoolSize = threadPoolSize;
		this.polling = new ArrayList<DataSourceVO<?>>();
	}

	/**
	 * Blocking method that will attempt to start all datasources in parallel using threadPoolSize number of threads at most.
	 * @return List of all data sources that need to begin polling.
	 */
	public List<DataSourceVO<?>> initialize() {
		
		long startTs = System.currentTimeMillis();
		if(this.group == null){
			if(this.useMetrics)
				LOG.info("Initialization of " + this.group.size() + " " + this.startPriority.name() +  " priority data sources took " + (System.currentTimeMillis() - startTs));
			return polling;
		}
		
		//Compute the size of the subGroup that each thread will use.
		int subGroupSize = this.group.size() / this.threadPoolSize;
		
		if(useMetrics)
			LOG.info("Initializing " + this.group.size() + " " + this.startPriority.name() + " priority data sources in " + this.threadPoolSize + " threads.");
		
		this.runningTasks = new ArrayList<DataSourceSubGroupInitializer>(this.threadPoolSize);
		//Add and Start the tasks 
		int endPos;
		for(int i=0; i<this.threadPoolSize; i++){
			if(i==this.threadPoolSize-1){
				//Last group may be larger
				endPos = this.group.size();
			}else{
				endPos = (i*subGroupSize) + subGroupSize;
			}
			DataSourceSubGroupInitializer currentSubgroup = new DataSourceSubGroupInitializer(this.group.subList(i*subGroupSize, endPos), this);
			
			synchronized(this.runningTasks){
				this.runningTasks.add(currentSubgroup);
			}
			Common.timer.execute(currentSubgroup);
		}
		
		//Wait here until all threads are finished
		while(runningTasks.size() > 0){
			try { Thread.sleep(100); } catch (InterruptedException e) { }
		}
		
		if(this.useMetrics)
			LOG.info("Initialization of " + this.group.size() + " " + this.startPriority.name() +  " priority data sources took " + (System.currentTimeMillis() - startTs));

		return polling;
	}

	public void addPollingDataSources(List<DataSourceVO<?>> vos){
		synchronized(this.polling){
			this.polling.addAll(vos);
		}
	}
	
	public void removeRunningTask(DataSourceSubGroupInitializer task){
		synchronized(this.runningTasks){
			this.runningTasks.remove(task);
		}
	}
	/**
	 * Initialize a sub group of the data sources in one thread.
	 * @author Terry Packer
	 *
	 */
	class DataSourceSubGroupInitializer extends TimerTask{
		
		private final Log LOG = LogFactory.getLog(DataSourceSubGroupInitializer.class);
		
		private List<DataSourceVO<?>> subgroup;
		private DataSourceGroupInitializer parent;
		
		public DataSourceSubGroupInitializer(List<DataSourceVO<?>> subgroup, DataSourceGroupInitializer parent){
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
				List<DataSourceVO<?>> polling = new ArrayList<DataSourceVO<?>>();
				for(DataSourceVO<?> config : subgroup){
					if(Common.runtimeManager.initializeDataSourceStartup(config))
						polling.add(config);
				}
				this.parent.addPollingDataSources(polling);
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
