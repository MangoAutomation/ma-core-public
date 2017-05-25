/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.util.timeout.TaskRejectionHandler;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.web.mvc.rest.v1.model.workitem.WorkItemModel;
import com.serotonin.provider.ProviderNotFoundException;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.OrderedRealTimeTimer;
import com.serotonin.timer.OrderedTaskInfo;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;
import com.serotonin.timer.TaskWrapper;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycle;

/**
 * A cheesy name for a class, i know, but it pretty much says it like it is. This class keeps an inbox of items to
 * process, and oddly enough, processes them. (Oh, and removes them from the inbox when it's done.)
 * 
 * @author Matthew Lohbihler
 */
public class BackgroundProcessing implements ILifecycle {
    final Log log = LogFactory.getLog(BackgroundProcessing.class);
    
    //Private access to our timer
    private OrderedRealTimeTimer timer;
    private OrderedThreadPoolExecutor highPriorityService;
    private TaskRejectionHandler highPriorityRejectionHandler;
    private TaskRejectionHandler mediumPriorityRejectionHandler;
    
    
    //Lower Limits on Pool Sizes for Mango To Run
    public static final int HIGH_PRI_MAX_POOL_SIZE_MIN = 5;
    public static final int MED_PRI_MAX_POOL_SIZE_MIN = 1;
    public static final int LOW_PRI_MAX_POOL_SIZE_MIN = 1;
    
    private OrderedThreadPoolExecutor mediumPriorityService;
    private ThreadPoolExecutor lowPriorityService;
    
    private int state = PRE_INITIALIZE;
    
    public BackgroundProcessing(){
    	try {
        	this.timer = (OrderedRealTimeTimer)Providers.get(TimerProvider.class).getTimer();
        	this.highPriorityService = (OrderedThreadPoolExecutor)timer.getExecutorService();
        }
        catch (ProviderNotFoundException e) {
            throw new ShouldNeverHappenException(e);
        }
    }


    /**
     * Execute a high priority task as soon as possible
     * @param task
     */
    public void execute(HighPriorityTask task){
    	this.timer.execute(task);
    }
    
	/**
	 * Schedule a timeout task to run at high priority
	 * @param task
	 */
	public void schedule(TimeoutTask task) {
		this.timer.schedule(task);
	}
    
	/**
	 * Schedule a timer task to run at high priority
	 * @param task
	 */
	public void schedule(TimerTask task) {
		this.timer.schedule(task);
	}
	
	/**
	 * Run tasks @ medium priority
	 * @param task
	 */
	public void executeMediumPriorityTask(TimerTask task){
		this.mediumPriorityService.execute(new TaskWrapper(task, this.timer.currentTimeMillis()));
	}
    		
    /**
     * add a work item int our queue
     * @param item
     */
    public void addWorkItem(final WorkItem item) {
        try{
	        if (item.getPriority() == WorkItem.PRIORITY_HIGH){
	        	timer.execute(new RejectableWorkItemRunnable(item, this.highPriorityRejectionHandler));
	        }
	        else if (item.getPriority() == WorkItem.PRIORITY_MEDIUM){
	            mediumPriorityService.execute(new TaskWrapper(new RejectableWorkItemRunnable(item, this.mediumPriorityRejectionHandler), this.timer.currentTimeMillis()));
	        }
	        else{
	            lowPriorityService.execute(new WorkItemRunnable(item));
	        }
        }catch(RejectedExecutionException e){
        	log.fatal(new TranslatableMessage("event.system.rejectedWorkItemMessage", e.getMessage()).translate(Common.getTranslations()), e);
        }
    }
    
    /**
     * A high priority task was rejected, track it
     * @param reason
     */
    public void rejectedHighPriorityTask(RejectedTaskReason reason){
    	highPriorityRejectionHandler.rejectedTask(reason);
    }
    
    
    
    /**
     * Return the count of all scheduled tasks ever
     * @return
     */
    public int getHighPriorityServiceScheduledTaskCount(){
    	return this.timer.size();
    }
    
    public int getHighPriorityServiceQueueSize(){
    	return highPriorityService.getQueue().size();
    }
    
    public int getHighPriorityServiceActiveCount(){
    	return highPriorityService.getActiveCount();
    }
    
    public int getHighPriorityServiceCorePoolSize(){
    	return highPriorityService.getCorePoolSize();
    }
    public int getHighPriorityServiceLargestPoolSize(){
    	return this.highPriorityService.getLargestPoolSize();
    }    
    
    public List<OrderedTaskInfo> getHighPriorityOrderedQueueStats(){
      	return this.highPriorityService.getOrderedQueueInfo();
    }
    
    /**
     * Set the core pool size for the high priority service.
     * The new size must be greater than the lowest allowable limit 
     * defined by HIGH_PRI_MAX_POOL_SIZE_MIN
     * @param size
     */
    public void setHighPriorityServiceCorePoolSize(int size){
    	if(size > HIGH_PRI_MAX_POOL_SIZE_MIN)
    		highPriorityService.setCorePoolSize(size);
    }

    /**
     * Set the maximum pool size for the high priority service.
     * The new size must be larger than the core pool size for this change to take 
     * effect.
     * @param size
     */
    public void setHighPriorityServiceMaximumPoolSize(int size){
		if(highPriorityService.getCorePoolSize() < size)
			highPriorityService.setMaximumPoolSize(size);
    }
    public int getHighPriorityServiceMaximumPoolSize(){
    	return highPriorityService.getMaximumPoolSize();
    }
    
    public Map<String, Integer> getHighPriorityServiceQueueClassCounts() {
    	Iterator<TimerTask> iter = timer.getTasks().iterator();
    	Map<String, Integer> classCounts = new HashMap<>();
    	while(iter.hasNext()){
    		TimerTask task = iter.next();
    		Integer count = classCounts.get(task.getName());
            if (count == null)
                count = 0;
            count++;
            classCounts.put(task.getName(), count);
    	}
    	return classCounts;
    }

    public int getMediumPriorityServiceQueueSize() {
        return mediumPriorityService.getQueue().size();
    }

    public Map<String, Integer> getMediumPriorityServiceQueueClassCounts() {
        return getClassCounts(mediumPriorityService);
    }

    public Map<String, Integer> getLowPriorityServiceQueueClassCounts() {
        return getClassCounts(lowPriorityService);
    }

    public List<OrderedTaskInfo> getMediumPriorityOrderedQueueStats(){
      	return this.mediumPriorityService.getOrderedQueueInfo();
    }
    
    public List<WorkItemModel> getHighPriorityServiceItems(){
    	List<WorkItemModel> list = new ArrayList<WorkItemModel>();
    	Iterator<TimerTask> iter = timer.getTasks().iterator();
    	while(iter.hasNext()){
    		TimerTask task = iter.next();
    		list.add(new WorkItemModel(task.getClass().getCanonicalName(), task.getName(), "HIGH"));
    	}
    	return list;
    }

    
    public List<WorkItemModel> getMediumPriorityServiceQueueItems(){
    	return getQueueItems(mediumPriorityService, "MEDIUM");
    	
    }
 
    
    
    /**
     * Set the Core Pool Size, in the medium priority queue this 
     * results in the maximum number of threads that will be run 
     * due to the way the pool is setup.  This will only set the pool size up to Maximum Pool Size
     * @param corePoolSize
     */
    public void setMediumPriorityServiceCorePoolSize(int corePoolSize){
    	if((corePoolSize > 0)&&(corePoolSize <= this.mediumPriorityService.getMaximumPoolSize()))
    		this.mediumPriorityService.setCorePoolSize(corePoolSize);
    }

    /**
     * Doesn't have any effect in the current configuration of the medium priority
     * queue
     * @param maximumPoolSize
     */
    public void setMediumPriorityServiceMaximumPoolSize(int maximumPoolSize){
    	if((maximumPoolSize >= MED_PRI_MAX_POOL_SIZE_MIN)&&(maximumPoolSize >= this.mediumPriorityService.getCorePoolSize())) //Default
    		this.mediumPriorityService.setMaximumPoolSize(maximumPoolSize);
    }
    
    public int getMediumPriorityServiceCorePoolSize(){
    	return this.mediumPriorityService.getCorePoolSize();
    }
    
    public int getMediumPriorityServiceMaximumPoolSize(){
    	return this.mediumPriorityService.getMaximumPoolSize();
    }
    
    public int getMediumPriorityServiceActiveCount(){
    	return this.mediumPriorityService.getActiveCount();
    }
    
    public int getMediumPriorityServiceLargestPoolSize(){
    	return this.mediumPriorityService.getLargestPoolSize();
    }    
    
    /**
     * Set the Core Pool Size, in the medium priority queue this 
     * results in the maximum number of threads that will be run 
     * due to the way the pool is setup.
     * @param corePoolSize
     */
    public void setLowPriorityServiceCorePoolSize(int corePoolSize){
    	if((corePoolSize > 0)&&(corePoolSize <= this.lowPriorityService.getMaximumPoolSize()))
    		this.lowPriorityService.setCorePoolSize(corePoolSize);
    }

    /**
     * Doesn't have any effect in the current configuration of the medium priority
     * queue
     * @param maximumPoolSize
     */
    public void setLowPriorityServiceMaximumPoolSize(int maximumPoolSize){
    	if((maximumPoolSize >= LOW_PRI_MAX_POOL_SIZE_MIN)&&maximumPoolSize >= this.lowPriorityService.getCorePoolSize()) //Default
    		this.lowPriorityService.setMaximumPoolSize(maximumPoolSize);
    }
    
    public int getLowPriorityServiceCorePoolSize(){
    	return this.lowPriorityService.getCorePoolSize();
    }
    
    public int getLowPriorityServiceMaximumPoolSize(){
    	return this.lowPriorityService.getMaximumPoolSize();
    }
    
    public int getLowPriorityServiceActiveCount(){
    	return this.lowPriorityService.getActiveCount();
    }
    
    public int getLowPriorityServiceLargestPoolSize(){
    	return this.lowPriorityService.getLargestPoolSize();
    }  
    
    public int getLowPriorityServiceQueueSize() {
        return lowPriorityService.getQueue().size();
    }
    
    public List<WorkItemModel> getLowPriorityServiceQueueItems(){
    	return getQueueItems(lowPriorityService, "LOW");
    }
    
    private Map<String, Integer> getClassCounts(ThreadPoolExecutor e) {
        Map<String, Integer> classCounts = new HashMap<>();
        Iterator<Runnable> iter = e.getQueue().iterator();
        while (iter.hasNext()) {
            Runnable r = iter.next();
            String s = r.toString();
            Integer count = classCounts.get(s);
            if (count == null)
                count = 0;
            count++;
            classCounts.put(s, count);
        }
        return classCounts;
    }
    
    private List<WorkItemModel> getQueueItems(ThreadPoolExecutor e, String priority){
    	List<WorkItemModel> list = new ArrayList<WorkItemModel>();
    	Iterator<Runnable> iter = e.getQueue().iterator();
    	while (iter.hasNext()) {
            Runnable r = iter.next();
            WorkItemRunnable wir = (WorkItemRunnable)r;
            list.add(new WorkItemModel(wir.getWorkItem().getClass().getCanonicalName(), wir.getWorkItem().getDescription(), priority));
        }
    	return list;
    }
    
    @Override
    public void initialize(boolean safe) {
        if (state != PRE_INITIALIZE)
            return;

        // Set the started indicator to true.
        state = INITIALIZE;
        
    	try {
        	this.timer = (OrderedRealTimeTimer)Providers.get(TimerProvider.class).getTimer();
        	this.highPriorityService = (OrderedThreadPoolExecutor)timer.getExecutorService();
        	this.highPriorityRejectionHandler = new TaskRejectionHandler();
        	this.mediumPriorityRejectionHandler = new TaskRejectionHandler();
        }
        catch (ProviderNotFoundException e) {
            throw new ShouldNeverHappenException(e);
        }
     	
    	//Adjust the high priority pool sizes now
    	int corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE);
    	int maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE);
    	this.highPriorityService.setCorePoolSize(corePoolSize);
    	this.highPriorityService.setMaximumPoolSize(maxPoolSize);
    	
    	//TODO Quick Fix for Setting default size somewhere other than in Lifecycle or Main
    	Common.defaultTaskQueueSize = Common.envProps.getInt("runtime.realTimeTimer.defaultTaskQueueSize", 1);
    	
    	//Pull our settings from the System Settings
    	corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE);
    	maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.MED_PRI_MAX_POOL_SIZE);
    	
    	//Sanity check to ensure the pool sizes are appropriate
    	if(maxPoolSize < MED_PRI_MAX_POOL_SIZE_MIN)
    		maxPoolSize = MED_PRI_MAX_POOL_SIZE_MIN;
    	if(maxPoolSize < corePoolSize)
    		maxPoolSize = corePoolSize;
    	mediumPriorityService = new OrderedThreadPoolExecutor(
    	       		corePoolSize,
    	       		maxPoolSize,
    	      		60L,
    	      		TimeUnit.SECONDS,
    	            new LinkedBlockingQueue<Runnable>(),
    	            new MangoThreadFactory("medium", Thread.MAX_PRIORITY - 2),
    	      		mediumPriorityRejectionHandler,
    	      		Common.envProps.getBoolean("runtime.realTimeTimer.flushTaskQueueOnReject", false));
        
    	corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE);
    	maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.LOW_PRI_MAX_POOL_SIZE);
    	//Sanity check to ensure the pool sizes are appropriate
    	if(maxPoolSize < LOW_PRI_MAX_POOL_SIZE_MIN)
    		maxPoolSize = LOW_PRI_MAX_POOL_SIZE_MIN;
    	if(maxPoolSize < corePoolSize)
    		maxPoolSize = corePoolSize;
        lowPriorityService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("low", Thread.NORM_PRIORITY));
        this.state = RUNNING;
    }

    @Override
    public void terminate() {
        if (state != RUNNING)
            return;
        state = TERMINATE;
        
        // Close the executor services.
    	if(highPriorityService != null){
        	//Terminate the RealTimeTimer
            if (Common.timer.isInitialized()) {
          	  List<TimerTask> tasks = Common.timer.cancel();
          	  for(TimerTask task : tasks)
          		  task.cancel();
            }
    		highPriorityService.shutdown();
    	}
    	if(mediumPriorityService != null)
    		mediumPriorityService.shutdown();
    	if(lowPriorityService != null)
    		lowPriorityService.shutdown();
    }

    @Override
    public void joinTermination() {
    	if(state != TERMINATE)
    		return;
    	state = POST_TERMINATE;
    	boolean highDone = false;
        boolean medDone = false;
        boolean lowDone = false;
        
        try {

            // With 5 second waits and a worst case of both of both high and low priority jobs that just won't finish,
            // this thread will wait a maximum of 6 minutes.
            int rewaits = Common.envProps.getInt("runtime.shutdown.medLowTimeout", 60);
            while (rewaits > 0) {
                if (!medDone && mediumPriorityService.awaitTermination(1, TimeUnit.SECONDS))
                    medDone = true;
                if (!lowDone && lowPriorityService.awaitTermination(1, TimeUnit.SECONDS))
                    lowDone = true;

                if (lowDone && medDone)
                    break;

                if ((!lowDone && !medDone)&&(rewaits % 5 == 0))
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + mediumPriorityService.getActiveCount() +
                            " active and " + mediumPriorityService.getQueue().size() + " queued medium priority tasks to complete.\n" + 
                            "BackgroundProcessing waiting " + rewaits + " more seconds for " + lowPriorityService.getActiveCount() +
                            " active and " + lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");
                else if ((!medDone)&&(rewaits % 5 == 0))
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + mediumPriorityService.getActiveCount() +
                            " active and " + mediumPriorityService.getQueue().size() + " queued medium priority tasks to complete.");
                else if(rewaits % 5 == 0)
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + lowPriorityService.getActiveCount() +
                            " active and " +lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");
                rewaits--;
            }
            
            //Wait for the high tasks now
            rewaits = Common.envProps.getInt("runtime.shutdown.highTimeout", 60) - rewaits;
            while(rewaits > 0){
            	if(!highDone && highPriorityService.awaitTermination(1, TimeUnit.SECONDS))
            		break;
            	if(rewaits % 5 == 0)
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + highPriorityService.getActiveCount() +
                            " active and " + highPriorityService.getQueue().size() + " queued high priority tasks to complete.");
            	
            	rewaits--;
            }
            
            List<Runnable> highTasks = highPriorityService.shutdownNow();
            if(highTasks.size() == 0)
            	log.info("All high priority tasks exited gracefully.");
            else
            	log.info(highTasks.size() + " high priority tasks forcefully terminated.");
            
            List<Runnable> medTasks = mediumPriorityService.shutdownNow();
            if(medTasks.size() == 0)
            	log.info("All medium priority tasks exited gracefully.");
            else
            	log.info(medTasks.size() + " medium priority tasks forcefully terminated.");
            List<Runnable> lowTasks = lowPriorityService.shutdownNow();
            if(lowTasks.size() == 0)
            	log.info("All low priority tasks exited gracefully.");
            else
            	log.info(lowTasks.size() + " low priority tasks forcefully terminated.");
        }
        catch (InterruptedException e) {
            log.info("", e);
        }
        state = TERMINATED;
    }
    
    public TaskRejectionHandler getHighPriorityRejectionHandler(){
    	return this.highPriorityRejectionHandler;
    }

    public TaskRejectionHandler getMediumPriorityRejectionHandler(){
    	return this.mediumPriorityRejectionHandler;
    }
    
    /**
     * Util to get the Current Thread Information
     * @param stackDepth - Depth to trace the stack
     * @return
     */
    public List<ThreadInfo> getThreadsList(int stackDepth){
    	
        // All of the last thread ids. Ids are removed from this set as they are processed. If ids remain,
        // it means the thread is gone and should be removed from the map.

        ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threads = tmxb.getThreadInfo(tmxb.getAllThreadIds(), Integer.MAX_VALUE);
        List<ThreadInfo> infos = new ArrayList<ThreadInfo>();
        for (ThreadInfo thread : threads) {
            if (thread == null)
                continue;
            infos.add(thread);
        }
		return infos;
    }
    
    /**
     * Helper class to get more info on Work Items while queued up
     * @author Terry Packer
     *
     */
    class RejectableWorkItemRunnable extends Task{

    	final WorkItem item;
    	final TaskRejectionHandler rejectionHandler;
    	
    	/**
		 * @param name
		 */
		public RejectableWorkItemRunnable(WorkItem item, TaskRejectionHandler rejectionHandler) {
			super(item.getDescription(), item.getTaskId(), item.getQueueSize());
			this.item = item;
			this.rejectionHandler = rejectionHandler;
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#run(long)
		 */
		@Override
		public void run(long runtime) {
            try {
                item.execute();
            }
            catch (Throwable t) {
            	log.error("Error in work item", t);
            }
        }
		
		public WorkItem getWorkItem(){
			return this.item;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return item.getDescription();
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
		 */
		@Override
		public void rejected(RejectedTaskReason reason) {
			item.rejected(reason);
			rejectionHandler.rejectedTask(reason);
		}
    }

    /**
     * Helper class to get more info on Work Items while queued up
     * @author Terry Packer
     *
     */
    class WorkItemRunnable implements Runnable{
    	
    	public WorkItemRunnable(WorkItem item){
    		this.item = item;
    	}
    	WorkItem item;
    	
    	@Override
        public void run() {
            try {
                item.execute();
            }
            catch (Throwable t) {
            	log.error("Error in work item", t);
            }
        }

		public WorkItem getWorkItem(){
			return this.item;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return item.getDescription();
		}
		public Class<?> getWorkItemClass() {
			return item.getClass();
		}

		public String getDescription() {
			return item.getDescription();
		}
    	
    }
    
}
