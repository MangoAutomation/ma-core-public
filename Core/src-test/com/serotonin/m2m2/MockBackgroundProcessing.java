/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.maint.BackgroundProcessingImpl;
import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.TaskRejectionHandler;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.TaskWrapper;
import com.serotonin.util.ILifecycleState;

/**
 * Dummy implementation of BackgrondProcessing for testing,
 *   override as necessary.
 *
 * @author Terry Packer
 */
public class MockBackgroundProcessing extends BackgroundProcessingImpl {
    
    public MockBackgroundProcessing() {
        this(null);
    }
    
    public MockBackgroundProcessing(AbstractTimer timer) {
        this.timer = timer;
    }

    @Override
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
            fail(e.getMessage());
        }
    }
    
    @Override
    public void initialize(boolean safe) {
        if(this.timer == null)
            this.timer = Common.timer;

        Common.defaultTaskQueueSize = Common.envProps.getInt("runtime.realTimeTimer.defaultTaskQueueSize", 1);
        this.highPriorityService = (OrderedThreadPoolExecutor)timer.getExecutorService();
        this.highPriorityRejectionHandler = new AssertTaskRejectionHandler();
        this.highPriorityService.setRejectedExecutionHandler(this.highPriorityRejectionHandler);
        //Adjust the high priority pool sizes now
        int corePoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE);
        int maxPoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE);
        this.highPriorityService.setCorePoolSize(corePoolSize);
        this.highPriorityService.setMaximumPoolSize(maxPoolSize);
        
        //Pull our settings from the System Settings
        corePoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE);
        
        //Sanity check to ensure the pool sizes are appropriate
        if(corePoolSize < MED_PRI_MAX_POOL_SIZE_MIN)
            corePoolSize = MED_PRI_MAX_POOL_SIZE_MIN;

        this.mediumPriorityRejectionHandler = new TaskRejectionHandler();
        this.mediumPriorityService = new OrderedThreadPoolExecutor(
                    corePoolSize,
                    corePoolSize,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    new MangoThreadFactory("medium", Thread.MAX_PRIORITY - 2, Thread.currentThread().getContextClassLoader()),
                    mediumPriorityRejectionHandler,
                    Common.envProps.getBoolean("runtime.realTimeTimer.flushTaskQueueOnReject", false),
                    Common.timer.getTimeSource());
        
        corePoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE);
        //Sanity check to ensure the pool sizes are appropriate
        if(corePoolSize < LOW_PRI_MAX_POOL_SIZE_MIN)
            corePoolSize = LOW_PRI_MAX_POOL_SIZE_MIN;
        this.lowPriorityService = new ThreadPoolExecutor(corePoolSize, corePoolSize, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("low", Thread.NORM_PRIORITY, Thread.currentThread().getContextClassLoader()));
        this.state = ILifecycleState.RUNNING;
    }

    @Override
    public void terminate() {
        this.timer.cancel();
        this.highPriorityService.shutdownNow();
        this.mediumPriorityService.shutdownNow();
        this.lowPriorityService.shutdownNow();
    }

    @Override
    public void joinTermination() {
        
    }

}
