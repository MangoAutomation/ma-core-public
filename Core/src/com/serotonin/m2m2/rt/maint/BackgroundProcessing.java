/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
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

        if (item.getPriority() == WorkItem.PRIORITY_HIGH)
            Common.timer.execute(runnable);
        else if (item.getPriority() == WorkItem.PRIORITY_MEDIUM)
            mediumPriorityService.execute(runnable);
        else
            lowPriorityService.execute(runnable);
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
    public List<WorkItemModel> getLowPriorityServiceQueueItems(){
    	return getQueueItems(lowPriorityService, "LOW");
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
        mediumPriorityService = new ThreadPoolExecutor(3, 30, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("medium"));
        mediumPriorityService.allowCoreThreadTimeOut(true);
        lowPriorityService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
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
     * Helper class to get more info on Work Items while queued up
     * @author Terry Packer
     *
     */
    abstract class WorkItemRunnable implements Runnable{

    	public abstract Class<?> getWorkItemClass();
    	
    	public abstract String getDescription();
    	
    }
    

}
