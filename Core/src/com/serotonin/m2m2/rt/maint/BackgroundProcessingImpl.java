/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.infiniteautomation.mango.util.WorkItemInfo;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.util.timeout.TaskRejectionHandler;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.provider.ProviderNotFoundException;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.OrderedTaskInfo;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;
import com.serotonin.timer.TaskWrapper;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycleState;

/**
 * A cheesy name for a class, i know, but it pretty much says it like it is. This class keeps an inbox of items to
 * process, and oddly enough, processes them. (Oh, and removes them from the inbox when it's done.)
 *
 * @author Matthew Lohbihler
 */
public class BackgroundProcessingImpl implements BackgroundProcessing {
    final Logger log = LoggerFactory.getLogger(BackgroundProcessingImpl.class);

    //Private access to our timer
    protected AbstractTimer timer;
    protected OrderedThreadPoolExecutor highPriorityService;
    protected TaskRejectionHandler highPriorityRejectionHandler;
    protected TaskRejectionHandler mediumPriorityRejectionHandler;
    protected OrderedThreadPoolExecutor mediumPriorityService;
    protected ThreadPoolExecutor lowPriorityService;

    protected ILifecycleState state = ILifecycleState.PRE_INITIALIZE;

    @Override
    public void execute(HighPriorityTask task){
        this.timer.execute(task);
    }

    @Override
    public void schedule(TimeoutTask task) {
        this.timer.schedule(task);
    }

    @Override
    public void schedule(TimerTask task) {
        this.timer.schedule(task);
    }

    @Override
    public void executeMediumPriorityTask(TimerTask task){
        this.mediumPriorityService.execute(new TaskWrapper(task, this.timer.currentTimeMillis()));
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
            log.error(new TranslatableMessage("event.system.rejectedWorkItemMessage", e.getMessage()).translate(Common.getTranslations()), e);
        }
    }

    @Override
    public void rejectedHighPriorityTask(RejectedTaskReason reason){
        highPriorityRejectionHandler.rejectedTask(reason);
    }

    @Override
    public int getHighPriorityServiceScheduledTaskCount(){
        return this.timer.size();
    }

    @Override
    public int getHighPriorityServiceQueueSize(){
        return highPriorityService.getQueue().size();
    }

    @Override
    public int getHighPriorityServiceActiveCount(){
        return highPriorityService.getActiveCount();
    }

    @Override
    public int getHighPriorityServiceCorePoolSize(){
        return highPriorityService.getCorePoolSize();
    }

    @Override
    public int getHighPriorityServiceLargestPoolSize(){
        return this.highPriorityService.getLargestPoolSize();
    }

    @Override
    public List<OrderedTaskInfo> getHighPriorityOrderedQueueStats(){
        return this.highPriorityService.getOrderedQueueInfo();
    }

    @Override
    public void setHighPriorityServiceCorePoolSize(int size){
        if(size > HIGH_PRI_MAX_POOL_SIZE_MIN)
            highPriorityService.setCorePoolSize(size);
    }

    @Override
    public void setHighPriorityServiceMaximumPoolSize(int size){
        if(highPriorityService.getCorePoolSize() < size)
            highPriorityService.setMaximumPoolSize(size);
    }

    @Override
    public int getHighPriorityServiceMaximumPoolSize(){
        return highPriorityService.getMaximumPoolSize();
    }

    @Override
    public Map<String, Integer> getHighPriorityServiceQueueClassCounts() {
        Iterator<TimerTask> iter = timer.getTasks().iterator();
        Map<String, Integer> classCounts = new HashMap<>();
        while (iter.hasNext()) {
            TimerTask task = iter.next();
            Integer count = classCounts.get(task.getName());
            if (count == null)
                count = 0;
            count++;
            classCounts.put(task.getName(), count);
        }
        return classCounts;
    }

    @Override
    public int getMediumPriorityServiceQueueSize() {
        return mediumPriorityService.getQueue().size();
    }

    @Override
    public Map<String, Integer> getMediumPriorityServiceQueueClassCounts() {
        return getClassCounts(mediumPriorityService);
    }

    @Override
    public Map<String, Integer> getLowPriorityServiceQueueClassCounts() {
        return getClassCounts(lowPriorityService);
    }

    @Override
    public List<OrderedTaskInfo> getMediumPriorityOrderedQueueStats(){
        return this.mediumPriorityService.getOrderedQueueInfo();
    }

    @Override
    public List<WorkItemInfo> getHighPriorityServiceItems(){
        List<WorkItemInfo> list = new ArrayList<WorkItemInfo>();
        Iterator<TimerTask> iter = timer.getTasks().iterator();
        while (iter.hasNext()) {
            TimerTask task = iter.next();
            list.add(new WorkItemInfo(task.getClass().getCanonicalName(), task.getName(), "HIGH"));
        }
        return list;
    }

    @Override
    public List<WorkItemInfo> getMediumPriorityServiceQueueItems(){
        return getQueueItems(mediumPriorityService, "MEDIUM");
    }

    @Override
    public void setMediumPriorityServiceCorePoolSize(int corePoolSize){
        if (corePoolSize > MED_PRI_MAX_POOL_SIZE_MIN) {
            if(corePoolSize == this.mediumPriorityService.getMaximumPoolSize())
                return;
            else if(corePoolSize > this.mediumPriorityService.getMaximumPoolSize()) {
                //Increasing pool
                this.mediumPriorityService.setMaximumPoolSize(corePoolSize);
                this.mediumPriorityService.setCorePoolSize(corePoolSize);
            }else {
                //Decreasing pool
                this.mediumPriorityService.setCorePoolSize(corePoolSize);
                this.mediumPriorityService.setMaximumPoolSize(corePoolSize);
            }
        }
    }

    @Override
    public int getMediumPriorityServiceCorePoolSize(){
        return this.mediumPriorityService.getCorePoolSize();
    }

    @Override
    public int getMediumPriorityServiceMaximumPoolSize(){
        return this.mediumPriorityService.getMaximumPoolSize();
    }

    @Override
    public int getMediumPriorityServiceActiveCount(){
        return this.mediumPriorityService.getActiveCount();
    }

    @Override
    public int getMediumPriorityServiceLargestPoolSize(){
        return this.mediumPriorityService.getLargestPoolSize();
    }

    @Override
    public void setLowPriorityServiceCorePoolSize(int corePoolSize){
        if (corePoolSize > LOW_PRI_MAX_POOL_SIZE_MIN) {
            if(corePoolSize == this.lowPriorityService.getMaximumPoolSize())
                return;
            else if(corePoolSize > this.lowPriorityService.getMaximumPoolSize()) {
                //Increasing pool
                this.lowPriorityService.setMaximumPoolSize(corePoolSize);
                this.lowPriorityService.setCorePoolSize(corePoolSize);
            }else {
                //Decreasing pool
                this.lowPriorityService.setCorePoolSize(corePoolSize);
                this.lowPriorityService.setMaximumPoolSize(corePoolSize);
            }
        }
    }

    @Override
    public int getLowPriorityServiceCorePoolSize(){
        return this.lowPriorityService.getCorePoolSize();
    }

    @Override
    public int getLowPriorityServiceMaximumPoolSize(){
        return this.lowPriorityService.getMaximumPoolSize();
    }

    @Override
    public int getLowPriorityServiceActiveCount(){
        return this.lowPriorityService.getActiveCount();
    }

    @Override
    public int getLowPriorityServiceLargestPoolSize(){
        return this.lowPriorityService.getLargestPoolSize();
    }

    @Override
    public int getLowPriorityServiceQueueSize() {
        return lowPriorityService.getQueue().size();
    }

    @Override
    public List<WorkItemInfo> getLowPriorityServiceQueueItems(){
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

    private List<WorkItemInfo> getQueueItems(ThreadPoolExecutor e, String priority){
        List<WorkItemInfo> list = new ArrayList<WorkItemInfo>();
        Iterator<Runnable> iter = e.getQueue().iterator();
        while (iter.hasNext()) {
            Runnable r = iter.next();
            WorkItemRunnable wir = (WorkItemRunnable)r;
            list.add(new WorkItemInfo(wir.getWorkItem().getClass().getCanonicalName(), wir.getWorkItem().getDescription(), priority));
        }
        return list;
    }

    @Override
    public ILifecycleState getLifecycleState() {
        return state;
    }

    @Override
    public void initialize(boolean safe) {
        ensureState(ILifecycleState.PRE_INITIALIZE);
        // Set the started indicator to true.
        state = ILifecycleState.INITIALIZING;

        try {
            this.timer = Providers.get(TimerProvider.class).getTimer();
            this.highPriorityService = (OrderedThreadPoolExecutor)timer.getExecutorService();
            this.highPriorityRejectionHandler = new TaskRejectionHandler();
            this.mediumPriorityRejectionHandler = new TaskRejectionHandler();
        }
        catch (ProviderNotFoundException e) {
            throw new ShouldNeverHappenException(e);
        }
        this.highPriorityService.setRejectedExecutionHandler(this.highPriorityRejectionHandler);

        //Adjust the high priority pool sizes now
        int maxPoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE);
        this.highPriorityService.setMaximumPoolSize(maxPoolSize);
        int corePoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE);
        this.highPriorityService.setCorePoolSize(corePoolSize);

        //TODO Quick Fix for Setting default size somewhere other than in Lifecycle or Main
        Common.defaultTaskQueueSize = Common.envProps.getInt("runtime.realTimeTimer.defaultTaskQueueSize", 1);

        //Pull our settings from the System Settings
        corePoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE);

        //Sanity check to ensure the pool sizes are appropriate
        if(corePoolSize < MED_PRI_MAX_POOL_SIZE_MIN)
            corePoolSize = MED_PRI_MAX_POOL_SIZE_MIN;
        mediumPriorityService = new OrderedThreadPoolExecutor(
                corePoolSize,
                corePoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new MangoThreadFactory("medium", Thread.MAX_PRIORITY - 2, Common.getModuleClassLoader()),
                mediumPriorityRejectionHandler,
                Common.envProps.getBoolean("runtime.realTimeTimer.flushTaskQueueOnReject", false),
                Common.timer.getTimeSource());

        corePoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE);
        //Sanity check to ensure the pool sizes are appropriate
        if(corePoolSize < LOW_PRI_MAX_POOL_SIZE_MIN)
            corePoolSize = LOW_PRI_MAX_POOL_SIZE_MIN;
        lowPriorityService = new ThreadPoolExecutor(corePoolSize, corePoolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new MangoThreadFactory("low", Thread.NORM_PRIORITY, Common.getModuleClassLoader()));
        this.state = ILifecycleState.RUNNING;
    }

    @Override
    public void terminate() {
        ensureState(ILifecycleState.RUNNING);
        state = ILifecycleState.TERMINATING;

        // Close the executor services.
        if(lowPriorityService != null) {
            lowPriorityService.shutdown();
        }

        if(mediumPriorityService != null) {
            mediumPriorityService.shutdown();
        }

        if(highPriorityService != null) {
            //Terminate the RealTimeTimer
            if (Common.timer.isInitialized()) {
                List<TimerTask> tasks = Common.timer.cancel();
                for(TimerTask task : tasks)
                    task.cancel();
            }
            highPriorityService.shutdown();
        }
    }

    @Override
    public void joinTermination() {
        if (state == ILifecycleState.TERMINATED) return;
        ensureState(ILifecycleState.TERMINATING);
        boolean medDone = false;
        boolean lowDone = false;

        try {

            // With 5 second waits and a worst case of both of both high and low priority jobs that just won't finish,
            // this thread will wait a maximum of 6 minutes.
            int rewaits = Common.envProps.getInt("runtime.shutdown.medLowTimeout", 60);
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

                if ((!lowDone && !medDone)&&(rewaits % 5 == 0)) {
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + mediumPriorityService.getActiveCount() +
                            " active and " + mediumPriorityService.getQueue().size() + " queued medium priority tasks to complete.\n" +
                            "BackgroundProcessing waiting " + rewaits + " more seconds for " + lowPriorityService.getActiveCount() +
                            " active and " + lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");
                } else if ((!medDone) && (rewaits % 5 == 0)) {
                        log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + mediumPriorityService.getActiveCount() +
                                " active and " + mediumPriorityService.getQueue().size() + " queued medium priority tasks to complete.");
                } else if(rewaits % 5 == 0) {
                log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + lowPriorityService.getActiveCount() +
                        " active and " + lowPriorityService.getQueue().size() + " queued low priority tasks to complete.");
                }
                rewaits--;
            }

            //Wait for the high tasks now
            rewaits = Common.envProps.getInt("runtime.shutdown.highTimeout", 60) - rewaits;
            while(rewaits > 0) {
                if(highPriorityService.awaitTermination(1, TimeUnit.SECONDS))
                    break;
                if(rewaits % 5 == 0)
                    log.info("BackgroundProcessing waiting " + rewaits + " more seconds for " + highPriorityService.getActiveCount() +
                            " active and " + highPriorityService.getQueue().size() + " queued high priority tasks to complete.");

                rewaits--;
            }

            List<Runnable> lowTasks = lowPriorityService.shutdownNow();
            if(lowTasks.size() == 0) {
                log.info("All low priority tasks exited gracefully.");
            } else {
                log.info(lowTasks.size() + " low priority tasks forcefully terminated.");
            }

            List<Runnable> medTasks = mediumPriorityService.shutdownNow();
            if(medTasks.size() == 0) {
                log.info("All medium priority tasks exited gracefully.");
            } else {
                log.info(medTasks.size() + " medium priority tasks forcefully terminated.");
            }

            List<Runnable> highTasks = highPriorityService.shutdownNow();
            if(highTasks.size() == 0) {
                log.info("All high priority tasks exited gracefully.");
            } else {
                log.info(highTasks.size() + " high priority tasks forcefully terminated.");
            }
        }
        catch (InterruptedException e) {
            log.info("Failure awaiting Background Processing termination", e);
        }
        state = ILifecycleState.TERMINATED;
    }

    @Override
    public TaskRejectionHandler getHighPriorityRejectionHandler(){
        return this.highPriorityRejectionHandler;
    }

    @Override
    public TaskRejectionHandler getMediumPriorityRejectionHandler(){
        return this.mediumPriorityRejectionHandler;
    }

    @Override
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
    public class RejectableWorkItemRunnable extends Task {

        final WorkItem item;
        final TaskRejectionHandler rejectionHandler;

        public RejectableWorkItemRunnable(WorkItem item, TaskRejectionHandler rejectionHandler) {
            super(item.getDescription(), item.getTaskId(), item.getQueueSize());
            this.item = item;
            this.rejectionHandler = rejectionHandler;
        }

        @Override
        public void run(long runtime) {
            try {
                item.execute();
            }
            catch (Exception t) {
                log.error("Error in work item", t);
            }
        }

        public WorkItem getWorkItem(){
            return this.item;
        }

        @Override
        public String toString() {
            return item.getDescription();
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            try {
                item.rejected(reason);
                rejectionHandler.rejectedTask(reason);
            }catch(Exception e){
                log.error("Uncaught work item rejection exception", e);
            }
        }
    }

    /**
     * Helper class to get more info on Work Items while queued up
     *
     * @author Terry Packer
     */
    public class WorkItemRunnable implements Runnable {

        private final WorkItem item;
        private final SecurityContext delegateSecurityContext;

        public WorkItemRunnable(WorkItem item) {
            this.item = item;
            this.delegateSecurityContext = SecurityContextHolder.getContext();
        }

        @Override
        public void run() {
            SecurityContext original = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(this.delegateSecurityContext);
            try {
                item.execute();
            } catch (Exception t) {
                String message = "Error in work item ";
                if (item.getDescription() != null) {
                    message += item.getDescription();
                }
                if (item.getTaskId() != null) {
                    message += " with id " + item.getTaskId();
                }
                log.error(message, t);
            } finally {
                SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
                if (emptyContext.equals(original)) {
                    SecurityContextHolder.clearContext();
                } else {
                    SecurityContextHolder.setContext(original);
                }
            }
        }

        public WorkItem getWorkItem() {
            return this.item;
        }

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
