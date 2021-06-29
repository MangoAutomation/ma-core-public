/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.maint;

import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.util.WorkItemInfo;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.util.timeout.TaskRejectionHandler;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.timer.OrderedTaskInfo;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycle;


/**
 *
 * @author Terry Packer
 */
public interface BackgroundProcessing extends ILifecycle {

    //Lower Limits on Pool Sizes for Mango To Run
    int HIGH_PRI_MAX_POOL_SIZE_MIN = 5;
    int MED_PRI_MAX_POOL_SIZE_MIN = 1;
    int LOW_PRI_MAX_POOL_SIZE_MIN = 1;

    /**
     * Execute a high priority task as soon as possible
     * @param task
     */
    void execute(HighPriorityTask task);

    /**
     * Schedule a timeout task to run at high priority
     * @param task
     */
    void schedule(TimeoutTask task);

    /**
     * Schedule a timer task to run at high priority
     * @param task
     */
    void schedule(TimerTask task);

    /**
     * Run tasks @ medium priority
     * @param task
     */
    void executeMediumPriorityTask(TimerTask task);

    /**
     * add a work item into our queue
     * @param item
     */
    void addWorkItem(WorkItem item);

    /**
     * A high priority task was rejected, track it
     * @param reason
     */
    void rejectedHighPriorityTask(RejectedTaskReason reason);

    /**
     * Return the count of all scheduled tasks ever
     * @return
     */
    int getHighPriorityServiceScheduledTaskCount();

    int getHighPriorityServiceQueueSize();

    int getHighPriorityServiceActiveCount();

    int getHighPriorityServiceCorePoolSize();

    int getHighPriorityServiceLargestPoolSize();

    List<OrderedTaskInfo> getHighPriorityOrderedQueueStats();

    /**
     * Set the core pool size for the high priority service.
     * The new size must be greater than the lowest allowable limit
     * defined by HIGH_PRI_MAX_POOL_SIZE_MIN
     * @param size
     */
    void setHighPriorityServiceCorePoolSize(int size);

    /**
     * Set the maximum pool size for the high priority service.
     * The new size must be larger than the core pool size for this change to take
     * effect.
     * @param size
     */
    void setHighPriorityServiceMaximumPoolSize(int size);

    int getHighPriorityServiceMaximumPoolSize();

    Map<String, Integer> getHighPriorityServiceQueueClassCounts();

    int getMediumPriorityServiceQueueSize();

    Map<String, Integer> getMediumPriorityServiceQueueClassCounts();

    Map<String, Integer> getLowPriorityServiceQueueClassCounts();

    List<OrderedTaskInfo> getMediumPriorityOrderedQueueStats();

    List<WorkItemInfo> getHighPriorityServiceItems();

    List<WorkItemInfo> getMediumPriorityServiceQueueItems();

    /**
     * Set the Core Pool Size, in the medium priority queue this
     * results in the maximum number of threads that will be run
     * due to the way the pool is setup.  This will only set the pool size up to Maximum Pool Size
     * @param corePoolSize
     */
    void setMediumPriorityServiceCorePoolSize(int corePoolSize);

    int getMediumPriorityServiceCorePoolSize();

    int getMediumPriorityServiceMaximumPoolSize();

    int getMediumPriorityServiceActiveCount();

    int getMediumPriorityServiceLargestPoolSize();

    /**
     * Set the Core Pool Size, in the medium priority queue this
     * results in the maximum number of threads that will be run
     * due to the way the pool is setup.
     * @param corePoolSize
     */
    void setLowPriorityServiceCorePoolSize(int corePoolSize);


    int getLowPriorityServiceCorePoolSize();

    int getLowPriorityServiceMaximumPoolSize();

    int getLowPriorityServiceActiveCount();

    int getLowPriorityServiceLargestPoolSize();

    int getLowPriorityServiceQueueSize();

    List<WorkItemInfo> getLowPriorityServiceQueueItems();

    //Lifecycle Interface
    @Override
    void initialize(boolean safe);

    @Override
    void terminate();

    @Override
    void joinTermination();

    TaskRejectionHandler getHighPriorityRejectionHandler();

    TaskRejectionHandler getMediumPriorityRejectionHandler();

    /**
     * Util to get the Current Thread Information
     * @param stackDepth - Depth to trace the stack
     * @return
     */
    List<ThreadInfo> getThreadsList(int stackDepth);

}
