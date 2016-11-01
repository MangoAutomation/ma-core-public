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
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.web.mvc.rest.v1.model.workitem.WorkItemModel;
import com.serotonin.provider.ProviderNotFoundException;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.OrderedRealTimeTimer;
import com.serotonin.timer.OrderedThreadPoolExecutor;
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
    
    //Lower Limits on Pool Sizes for Mango To Run
    public static final int HIGH_PRI_MAX_POOL_SIZE_MIN = 5;
    public static final int MED_PRI_MAX_POOL_SIZE_MIN = 1;
    public static final int LOW_PRI_MAX_POOL_SIZE_MIN = 1;
    
    private OrderedRealTimeTimer timer;
    private OrderedThreadPoolExecutor highPriorityService;
    
    private ThreadPoolExecutor mediumPriorityService;
    private ThreadPoolExecutor lowPriorityService;
    
    public BackgroundProcessing(){
    	try {
        	this.timer = (OrderedRealTimeTimer)Providers.get(TimerProvider.class).getTimer();
        	this.highPriorityService = (OrderedThreadPoolExecutor)timer.getExecutorService();
        }
        catch (ProviderNotFoundException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public void addWorkItem(final WorkItem item) {
        Runnable runnable = new WorkItemRunnable() {
            @Override
            public void run() {
                try {
                    item.execute();
                }
                catch (Throwable t) {
                	log.error("Error in work item", t);
                }
            }

            @Override
            public String toString() {
                return item.getDescription();
            }

			@Override
			public Class<?> getWorkItemClass() {
				return item.getClass();
			}

			@Override
			public String getDescription() {
				return item.getDescription();
			}
        };
        try{
	        if (item.getPriority() == WorkItem.PRIORITY_HIGH)
	            Common.timer.execute(runnable);
	        else if (item.getPriority() == WorkItem.PRIORITY_MEDIUM)
	            mediumPriorityService.execute(runnable);
	        else
	            lowPriorityService.execute(runnable);
        }catch(RejectedExecutionException e){
        	// Notify the event manager of the problem
            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_REJECTED_WORK_ITEM), 
            		System.currentTimeMillis(), false, new TranslatableMessage("event.system.rejectedWorkItemMessage", e.getMessage()));
        	throw e;
        }
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
    
    public int getMediumPriorityServiceQueueSize() {
        return mediumPriorityService.getQueue().size();
    }

    public Map<String, Integer> getHighPriorityServiceQueueClassCounts() {
    	Iterator<TimerTask> iter = Common.timer.getTasks().iterator();
    	Map<String, Integer> classCounts = new HashMap<>();
    	while(iter.hasNext()){
    		TimerTask task = iter.next();
    		String s = task.getClass().getCanonicalName();
    		Integer count = classCounts.get(s);
            if (count == null)
                count = 0;
            count++;
            classCounts.put(s, count);
    	}
    	return classCounts;
    }
    
    public Map<String, Integer> getMediumPriorityServiceQueueClassCounts() {
        return getClassCounts(mediumPriorityService);
    }

    public Map<String, Integer> getLowPriorityServiceQueueClassCounts() {
        return getClassCounts(lowPriorityService);
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

    public List<WorkItemModel> getHighPriorityServiceItems(){
    	List<WorkItemModel> list = new ArrayList<WorkItemModel>();
    	Iterator<TimerTask> iter = Common.timer.getTasks().iterator();
    	while(iter.hasNext()){
    		TimerTask task = iter.next();
    		String name = task.getClass().getCanonicalName();
    		list.add(new WorkItemModel(name, task.toString(), "HIGH"));
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
    
    public List<WorkItemModel> getLowPriorityServiceQueueItems(){
    	return getQueueItems(lowPriorityService, "LOW");
    }
    
    public int getLowPriorityServiceQueueSize() {
        return lowPriorityService.getQueue().size();
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
    
    private List<WorkItemModel> getQueueItems(ThreadPoolExecutor e, String priority){
    	List<WorkItemModel> list = new ArrayList<WorkItemModel>();
    	Iterator<Runnable> iter = e.getQueue().iterator();
    	while (iter.hasNext()) {
            Runnable r = iter.next();
            WorkItemRunnable wir = (WorkItemRunnable)r;
            list.add(new WorkItemModel(wir.getWorkItemClass().getCanonicalName(), wir.getDescription(), priority));
        }
    	return list;
    }
    @Override
    public void initialize() {
    	
    	//Adjust the RealTime timer pool now
    	int corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE);
    	int maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE);
    	
    	//Sanity check to ensure the pool sizes are appropriate
    	if(maxPoolSize < HIGH_PRI_MAX_POOL_SIZE_MIN)
    		maxPoolSize = HIGH_PRI_MAX_POOL_SIZE_MIN;
    	if(maxPoolSize < corePoolSize)
    		maxPoolSize = corePoolSize;
    	ThreadPoolExecutor executor = (ThreadPoolExecutor) Common.timer.getExecutorService();
    	executor.setCorePoolSize(corePoolSize);
    	executor.setMaximumPoolSize(maxPoolSize);
    	
    	//Pull our settings from the System Settings
    	corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE);
    	maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.MED_PRI_MAX_POOL_SIZE);
    	
    	//Sanity check to ensure the pool sizes are appropriate
    	if(maxPoolSize < MED_PRI_MAX_POOL_SIZE_MIN)
    		maxPoolSize = MED_PRI_MAX_POOL_SIZE_MIN;
    	if(maxPoolSize < corePoolSize)
    		maxPoolSize = corePoolSize;
        mediumPriorityService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("medium", Thread.NORM_PRIORITY));
        
    	corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE);
    	maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.LOW_PRI_MAX_POOL_SIZE);
    	//Sanity check to ensure the pool sizes are appropriate
    	if(maxPoolSize < LOW_PRI_MAX_POOL_SIZE_MIN)
    		maxPoolSize = LOW_PRI_MAX_POOL_SIZE_MIN;
    	if(maxPoolSize < corePoolSize)
    		maxPoolSize = corePoolSize;
        lowPriorityService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("low", Thread.NORM_PRIORITY));
    }

    @Override
    public void terminate() {
        // Close the executor services.
    	if(mediumPriorityService != null)
    		mediumPriorityService.shutdown();
    	if(lowPriorityService != null)
    		lowPriorityService.shutdown();
    }

    @Override
    public void joinTermination() {
        boolean medDone = false;
        if(mediumPriorityService == null)
        	medDone = true;
        boolean lowDone = false;
        if(lowPriorityService == null)
        	lowDone = true;
        
        if(lowDone && medDone)
        	return;
        
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
                            " active and " +lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");
                else if ((!medDone)&&(rewaits % 5 == 0))
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + mediumPriorityService.getActiveCount() +
                            " active and " + mediumPriorityService.getQueue().size() + " queued medium priority tasks to complete.");
                else if(rewaits % 5 == 0)
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + lowPriorityService.getActiveCount() +
                            " active and " +lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");

                rewaits--;
            }
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
     * Get the timer's current time
     * @return
     */
    public long currentTimeMillis(){
    	return this.timer.currentTimeMillis();
    }
    
    /**
     * Helper class to get more info on Work Items while queued up
     * @author Terry Packer
     *
     */
    abstract class WorkItemRunnable implements Runnable{

    	public abstract Class<?> getWorkItemClass();
    	
    	public abstract String getDescription();
    	
    }
    

}
