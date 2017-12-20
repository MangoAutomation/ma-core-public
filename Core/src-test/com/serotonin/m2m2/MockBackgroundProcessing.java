/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.util.timeout.TaskRejectionHandler;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.web.mvc.rest.v1.model.workitem.WorkItemModel;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.OrderedTaskInfo;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;
import com.serotonin.timer.TimerTask;

/**
 * Dummy implementation of BackgrondProcessing for testing,
 *   override as necessary.
 *
 * @author Terry Packer
 */
public class MockBackgroundProcessing implements BackgroundProcessing{
    
    protected AbstractTimer timer;
    
    public MockBackgroundProcessing() {
        this(null);
    }
    public MockBackgroundProcessing(AbstractTimer timer) {
        this.timer = timer;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#execute(com.serotonin.m2m2.util.timeout.HighPriorityTask)
     */
    @Override
    public void execute(HighPriorityTask task) {
        if(this.timer == null)
            Common.timer.execute(task);
        else
            this.timer.execute(task);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#schedule(com.serotonin.m2m2.util.timeout.TimeoutTask)
     */
    @Override
    public void schedule(TimeoutTask task) {
        if(this.timer == null)
            Common.timer.schedule(task);
        else
            this.timer.schedule(task);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#schedule(com.serotonin.timer.TimerTask)
     */
    @Override
    public void schedule(TimerTask task) {
        if(this.timer == null)
            Common.timer.schedule(task);
        else
            this.timer.schedule(task);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#executeMediumPriorityTask(com.serotonin.timer.TimerTask)
     */
    @Override
    public void executeMediumPriorityTask(TimerTask task) {
        throw new ShouldNeverHappenException("unimplemented yet.");
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#addWorkItem(com.serotonin.m2m2.rt.maint.work.WorkItem)
     */
    @Override
    public void addWorkItem(WorkItem item) {
        AbstractTimer timer = null;
        if(this.timer == null)
            timer = Common.timer;
        else
            timer = this.timer;
        
        try {
            if (item.getPriority() == WorkItem.PRIORITY_HIGH){
                
                timer.execute(new Task(item.getTaskId()) {

                    @Override
                    public void run(long runtime) {
                        item.execute();
                    }

                    @Override
                    public void rejected(RejectedTaskReason reason) {
                        fail(reason.toString());
                    }
                    
                });
            }
            else if (item.getPriority() == WorkItem.PRIORITY_MEDIUM){
                timer.execute(new Task(item.getTaskId()) {

                    @Override
                    public void run(long runtime) {
                        item.execute();
                    }

                    @Override
                    public void rejected(RejectedTaskReason reason) {
                        fail(reason.toString());
                    }
                    
                });            }
            else{
                timer.execute(new Task(item.getTaskId()) {

                    @Override
                    public void run(long runtime) {
                        item.execute();
                    }

                    @Override
                    public void rejected(RejectedTaskReason reason) {
                        fail(reason.toString());
                    }
                    
                });            }
        }catch(RejectedExecutionException e){
            throw new ShouldNeverHappenException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#rejectedHighPriorityTask(com.serotonin.timer.RejectedTaskReason)
     */
    @Override
    public void rejectedHighPriorityTask(RejectedTaskReason reason) {
        throw new ShouldNeverHappenException("unimplemented yet.");
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceScheduledTaskCount()
     */
    @Override
    public int getHighPriorityServiceScheduledTaskCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceQueueSize()
     */
    @Override
    public int getHighPriorityServiceQueueSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceActiveCount()
     */
    @Override
    public int getHighPriorityServiceActiveCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceCorePoolSize()
     */
    @Override
    public int getHighPriorityServiceCorePoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceLargestPoolSize()
     */
    @Override
    public int getHighPriorityServiceLargestPoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityOrderedQueueStats()
     */
    @Override
    public List<OrderedTaskInfo> getHighPriorityOrderedQueueStats() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#setHighPriorityServiceCorePoolSize(int)
     */
    @Override
    public void setHighPriorityServiceCorePoolSize(int size) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#setHighPriorityServiceMaximumPoolSize(int)
     */
    @Override
    public void setHighPriorityServiceMaximumPoolSize(int size) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceMaximumPoolSize()
     */
    @Override
    public int getHighPriorityServiceMaximumPoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceQueueClassCounts()
     */
    @Override
    public Map<String, Integer> getHighPriorityServiceQueueClassCounts() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityServiceQueueSize()
     */
    @Override
    public int getMediumPriorityServiceQueueSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityServiceQueueClassCounts()
     */
    @Override
    public Map<String, Integer> getMediumPriorityServiceQueueClassCounts() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getLowPriorityServiceQueueClassCounts()
     */
    @Override
    public Map<String, Integer> getLowPriorityServiceQueueClassCounts() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityOrderedQueueStats()
     */
    @Override
    public List<OrderedTaskInfo> getMediumPriorityOrderedQueueStats() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityServiceItems()
     */
    @Override
    public List<WorkItemModel> getHighPriorityServiceItems() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityServiceQueueItems()
     */
    @Override
    public List<WorkItemModel> getMediumPriorityServiceQueueItems() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#setMediumPriorityServiceCorePoolSize(int)
     */
    @Override
    public void setMediumPriorityServiceCorePoolSize(int corePoolSize) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityServiceCorePoolSize()
     */
    @Override
    public int getMediumPriorityServiceCorePoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityServiceMaximumPoolSize()
     */
    @Override
    public int getMediumPriorityServiceMaximumPoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityServiceActiveCount()
     */
    @Override
    public int getMediumPriorityServiceActiveCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityServiceLargestPoolSize()
     */
    @Override
    public int getMediumPriorityServiceLargestPoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#setLowPriorityServiceCorePoolSize(int)
     */
    @Override
    public void setLowPriorityServiceCorePoolSize(int corePoolSize) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getLowPriorityServiceCorePoolSize()
     */
    @Override
    public int getLowPriorityServiceCorePoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getLowPriorityServiceMaximumPoolSize()
     */
    @Override
    public int getLowPriorityServiceMaximumPoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getLowPriorityServiceActiveCount()
     */
    @Override
    public int getLowPriorityServiceActiveCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getLowPriorityServiceLargestPoolSize()
     */
    @Override
    public int getLowPriorityServiceLargestPoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getLowPriorityServiceQueueSize()
     */
    @Override
    public int getLowPriorityServiceQueueSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getLowPriorityServiceQueueItems()
     */
    @Override
    public List<WorkItemModel> getLowPriorityServiceQueueItems() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#initialize(boolean)
     */
    @Override
    public void initialize(boolean safe) {

    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#terminate()
     */
    @Override
    public void terminate() {
        this.timer.cancel();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#joinTermination()
     */
    @Override
    public void joinTermination() {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getHighPriorityRejectionHandler()
     */
    @Override
    public TaskRejectionHandler getHighPriorityRejectionHandler() {
        throw new ShouldNeverHappenException("unimplemented yet.");
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getMediumPriorityRejectionHandler()
     */
    @Override
    public TaskRejectionHandler getMediumPriorityRejectionHandler() {
        throw new ShouldNeverHappenException("unimplemented yet.");
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackgroundProcessing#getThreadsList(int)
     */
    @Override
    public List<ThreadInfo> getThreadsList(int stackDepth) {
        throw new ShouldNeverHappenException("unimplemented yet.");
    }
}
