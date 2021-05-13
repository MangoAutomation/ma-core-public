/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.maint.BackgroundProcessingImpl;
import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.TaskRejectionHandler;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.TaskWrapper;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycleState;

/**
 * Dummy implementation of BackgrondProcessing for testing,
 *   override as necessary.
 *
 * @author Terry Packer
 */
public class MockBackgroundProcessing extends BackgroundProcessingImpl {
    final Log log = LogFactory.getLog(MockBackgroundProcessing.class);

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
        //Terminate the RealTimeTimer
        if (this.timer.isInitialized()) {
            List<TimerTask> tasks = this.timer.cancel();
            for(TimerTask task : tasks)
                task.cancel();
        }
        highPriorityService.shutdown();
        this.highPriorityService.shutdownNow();

        this.mediumPriorityService.shutdown();
        this.mediumPriorityService.shutdownNow();

        this.lowPriorityService.shutdown();
        this.lowPriorityService.shutdownNow();
    }

    @Override
    public void joinTermination() {
        int rewaits = 5;
        boolean medDone = false;
        boolean lowDone = false;

        try {
            while (rewaits > 0) {
                if (!lowDone && lowPriorityService.awaitTermination(1, TimeUnit.SECONDS)) {
                    lowDone = true;
                }

                if (!medDone && mediumPriorityService.awaitTermination(1, TimeUnit.SECONDS)) {
                    medDone = true;
                }

                if (lowDone && medDone) {
                    break;
                }

                if ((!lowDone && !medDone) && (rewaits % 5 == 0)) {
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + mediumPriorityService.getActiveCount() +
                            " active and " + mediumPriorityService.getQueue().size() + " queued medium priority tasks to complete.\n" +
                            "BackgroundProcessing waiting " + rewaits + " more seconds for " + lowPriorityService.getActiveCount() +
                            " active and " + lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");
                } else if ((!medDone) && (rewaits % 5 == 0)) {
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + mediumPriorityService.getActiveCount() +
                            " active and " + mediumPriorityService.getQueue().size() + " queued medium priority tasks to complete.");
                } else if (rewaits % 5 == 0) {
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + lowPriorityService.getActiveCount() +
                            " active and " + lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");
                }
                rewaits--;
            }

            //Wait for the high tasks now
            rewaits = Common.envProps.getInt("runtime.shutdown.highTimeout", 60) - rewaits;
            while (rewaits > 0) {
                if (highPriorityService.awaitTermination(1, TimeUnit.SECONDS))
                    break;
                if (rewaits % 5 == 0)
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + highPriorityService.getActiveCount() +
                            " active and " + highPriorityService.getQueue().size() + " queued high priority tasks to complete.");

                rewaits--;
            }

            List<Runnable> lowTasks = lowPriorityService.shutdownNow();
            if (lowTasks.size() == 0) {
                log.info("All low priority tasks exited gracefully.");
            } else {
                log.info(lowTasks.size() + " low priority tasks forcefully terminated.");
            }

            List<Runnable> medTasks = mediumPriorityService.shutdownNow();
            if (medTasks.size() == 0) {
                log.info("All medium priority tasks exited gracefully.");
            } else {
                log.info(medTasks.size() + " medium priority tasks forcefully terminated.");
            }

            List<Runnable> highTasks = highPriorityService.shutdownNow();
            if (highTasks.size() == 0) {
                log.info("All high priority tasks exited gracefully.");
            } else {
                log.info(highTasks.size() + " high priority tasks forcefully terminated.");
            }
        }catch(Exception e) {
            log.info("Failure awaiting Mock Background Processing termination", e);
        }
    }

}
