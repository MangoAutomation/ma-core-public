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

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.web.mvc.rest.v1.model.WorkItemModel;
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

    private ThreadPoolExecutor mediumPriorityService;
    private ThreadPoolExecutor lowPriorityService;

    public void addWorkItem(final WorkItem item) {
        Runnable runnable = new WorkItemRunnable() {
            @Override
            public void run() {
                try {
                    item.execute();
                }
                catch (Throwable t) {
                    try {
                        log.error("Error in work item", t);
                    }
                    catch (RuntimeException e) {
                        t.printStackTrace();
                    }
                }
            }

            @Override
            public String toString() {
                return item.getClass().getCanonicalName();
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
    		list.add(new WorkItemModel(task.getClass().getCanonicalName(), task.toString(), "HIGH"));
    	}
    	return list;
    }

    
    public List<WorkItemModel> getMediumPriorityServiceQueueItems(){
    	return getQueueItems(mediumPriorityService, "MEDIUM");
    	
    }
 
    /**
     * Set the Core Pool Size, in the medium priority queue this 
     * results in the maximum number of threads that will be run 
     * due to the way the pool is setup.
     * @param corePoolSize
     */
    public void setMediumPriorityServiceCorePoolSize(int corePoolSize){
    	if(corePoolSize > 0) //Default is 3
    		this.mediumPriorityService.setCorePoolSize(corePoolSize);
    }

    /**
     * Doesn't have any effect in the current configuration of the medium priority
     * queue
     * @param maximumPoolSize
     */
    public void setMediumPriorityServiceMaximumPoolSize(int maximumPoolSize){
    	if(maximumPoolSize >= this.mediumPriorityService.getCorePoolSize()) //Default
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
    
    /**
     * Set the Core Pool Size, in the medium priority queue this 
     * results in the maximum number of threads that will be run 
     * due to the way the pool is setup.
     * @param corePoolSize
     */
    public void setLowPriorityServiceCorePoolSize(int corePoolSize){
    	if(corePoolSize > 0) //Default is 1
    		this.lowPriorityService.setCorePoolSize(corePoolSize);
    }

    /**
     * Doesn't have any effect in the current configuration of the medium priority
     * queue
     * @param maximumPoolSize
     */
    public void setLowPriorityServiceMaximumPoolSize(int maximumPoolSize){
    	if(maximumPoolSize >= this.lowPriorityService.getCorePoolSize()) //Default
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
    	int corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE, 0);
    	int maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE, 100);
    	ThreadPoolExecutor executor = (ThreadPoolExecutor) Common.timer.getExecutorService();
    	executor.setCorePoolSize(corePoolSize);
    	executor.setMaximumPoolSize(maxPoolSize);
    	
    	//Pull our settings from the System Settings
    	corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE, 3);
    	maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.MED_PRI_MAX_POOL_SIZE, 30);
        mediumPriorityService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("medium"));
        mediumPriorityService.allowCoreThreadTimeOut(true);
        
    	corePoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE, 3);
    	maxPoolSize = SystemSettingsDao.getIntValue(SystemSettingsDao.LOW_PRI_MAX_POOL_SIZE, 30);
        lowPriorityService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("low"));
    }

    @Override
    public void terminate() {
        // Close the executor services.
        mediumPriorityService.shutdown();
        lowPriorityService.shutdown();
    }

    @Override
    public void joinTermination() {
        boolean medDone = false;
        boolean lowDone = false;

        try {
            // With 5 second waits and a worst case of both of both high and low priority jobs that just won't finish,
            // this thread will wait a maximum of 6 minutes.
            int rewaits = 36;
            while (rewaits > 0) {
                if (!medDone && mediumPriorityService.awaitTermination(5, TimeUnit.SECONDS))
                    medDone = true;
                if (!lowDone && lowPriorityService.awaitTermination(5, TimeUnit.SECONDS))
                    lowDone = true;

                if (lowDone && medDone)
                    break;

                if (!lowDone && !medDone)
                    log.info("BackgroundProcessing waiting for medium (" + mediumPriorityService.getActiveCount() + ","
                            + mediumPriorityService.getQueue().size() + ") and low priority tasks to complete");
                else if (!medDone)
                    log.info("BackgroundProcessing waiting for medium priority tasks ("
                            + mediumPriorityService.getActiveCount() + "," + mediumPriorityService.getQueue().size()
                            + ") to complete");
                else
                    log.info("BackgroundProcessing waiting for low priority tasks to complete");

                rewaits--;
            }
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
     * Helper class to get more info on Work Items while queued up
     * @author Terry Packer
     *
     */
    abstract class WorkItemRunnable implements Runnable{

    	public abstract Class<?> getWorkItemClass();
    	
    	public abstract String getDescription();
    	
    }
    

}
