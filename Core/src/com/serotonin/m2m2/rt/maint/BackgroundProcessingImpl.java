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
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.OrderedTaskInfo;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;
import com.serotonin.timer.TaskWrapper;
import com.serotonin.timer.TimerTask;

/**
 * A cheesy name for a class, i know, but it pretty much says it like it is. This class keeps an inbox of items to
 * process, and oddly enough, processes them. (Oh, and removes them from the inbox when it's done.)
 *
 * @author Matthew Lohbihler
 */
public class BackgroundProcessingImpl implements BackgroundProcessing {
    final Log log = LogFactory.getLog(BackgroundProcessingImpl.class);

    //Private access to our timer
    protected AbstractTimer timer;
    protected OrderedThreadPoolExecutor highPriorityService;
    protected TaskRejectionHandler highPriorityRejectionHandler;
    protected TaskRejectionHandler mediumPriorityRejectionHandler;
    protected OrderedThreadPoolExecutor mediumPriorityService;
    protected ThreadPoolExecutor lowPriorityService;

    protected int state = PRE_INITIALIZE;

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#execute(com.serotonin.m2m2.util.timeout.HighPriorityTask)
     */
    @Override
    public void execute(HighPriorityTask task){
        this.timer.execute(task);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#schedule(com.serotonin.m2m2.util.timeout.TimeoutTask)
     */
    @Override
    public void schedule(TimeoutTask task) {
        this.timer.schedule(task);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#schedule(com.serotonin.timer.TimerTask)
     */
    @Override
    public void schedule(TimerTask task) {
        this.timer.schedule(task);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#executeMediumPriorityTask(com.serotonin.timer.TimerTask)
     */
    @Override
    public void executeMediumPriorityTask(TimerTask task){
        this.mediumPriorityService.execute(new TaskWrapper(task, this.timer.currentTimeMillis()));
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#addWorkItem(com.serotonin.m2m2.rt.maint.work.WorkItem)
     */
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
            log.fatal(new TranslatableMessage("event.system.rejectedWorkItemMessage", e.getMessage()).translate(Common.getTranslations()), e);
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#rejectedHighPriorityTask(com.serotonin.timer.RejectedTaskReason)
     */
    @Override
    public void rejectedHighPriorityTask(RejectedTaskReason reason){
        highPriorityRejectionHandler.rejectedTask(reason);
    }



    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceScheduledTaskCount()
     */
    @Override
    public int getHighPriorityServiceScheduledTaskCount(){
        return this.timer.size();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceQueueSize()
     */
    @Override
    public int getHighPriorityServiceQueueSize(){
        return highPriorityService.getQueue().size();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceActiveCount()
     */
    @Override
    public int getHighPriorityServiceActiveCount(){
        return highPriorityService.getActiveCount();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceCorePoolSize()
     */
    @Override
    public int getHighPriorityServiceCorePoolSize(){
        return highPriorityService.getCorePoolSize();
    }
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceLargestPoolSize()
     */
    @Override
    public int getHighPriorityServiceLargestPoolSize(){
        return this.highPriorityService.getLargestPoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityOrderedQueueStats()
     */
    @Override
    public List<OrderedTaskInfo> getHighPriorityOrderedQueueStats(){
        return this.highPriorityService.getOrderedQueueInfo();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#setHighPriorityServiceCorePoolSize(int)
     */
    @Override
    public void setHighPriorityServiceCorePoolSize(int size){
        if(size > HIGH_PRI_MAX_POOL_SIZE_MIN)
            highPriorityService.setCorePoolSize(size);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#setHighPriorityServiceMaximumPoolSize(int)
     */
    @Override
    public void setHighPriorityServiceMaximumPoolSize(int size){
        if(highPriorityService.getCorePoolSize() < size)
            highPriorityService.setMaximumPoolSize(size);
    }
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceMaximumPoolSize()
     */
    @Override
    public int getHighPriorityServiceMaximumPoolSize(){
        return highPriorityService.getMaximumPoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceQueueClassCounts()
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityServiceQueueSize()
     */
    @Override
    public int getMediumPriorityServiceQueueSize() {
        return mediumPriorityService.getQueue().size();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityServiceQueueClassCounts()
     */
    @Override
    public Map<String, Integer> getMediumPriorityServiceQueueClassCounts() {
        return getClassCounts(mediumPriorityService);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getLowPriorityServiceQueueClassCounts()
     */
    @Override
    public Map<String, Integer> getLowPriorityServiceQueueClassCounts() {
        return getClassCounts(lowPriorityService);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityOrderedQueueStats()
     */
    @Override
    public List<OrderedTaskInfo> getMediumPriorityOrderedQueueStats(){
        return this.mediumPriorityService.getOrderedQueueInfo();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityServiceItems()
     */
    @Override
    public List<WorkItemModel> getHighPriorityServiceItems(){
        List<WorkItemModel> list = new ArrayList<WorkItemModel>();
        Iterator<TimerTask> iter = timer.getTasks().iterator();
        while (iter.hasNext()) {
            TimerTask task = iter.next();
            list.add(new WorkItemModel(task.getClass().getCanonicalName(), task.getName(), "HIGH"));
        }
        return list;
    }


    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityServiceQueueItems()
     */
    @Override
    public List<WorkItemModel> getMediumPriorityServiceQueueItems(){
        return getQueueItems(mediumPriorityService, "MEDIUM");
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#setMediumPriorityServiceCorePoolSize(int)
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityServiceCorePoolSize()
     */
    @Override
    public int getMediumPriorityServiceCorePoolSize(){
        return this.mediumPriorityService.getCorePoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityServiceMaximumPoolSize()
     */
    @Override
    public int getMediumPriorityServiceMaximumPoolSize(){
        return this.mediumPriorityService.getMaximumPoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityServiceActiveCount()
     */
    @Override
    public int getMediumPriorityServiceActiveCount(){
        return this.mediumPriorityService.getActiveCount();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityServiceLargestPoolSize()
     */
    @Override
    public int getMediumPriorityServiceLargestPoolSize(){
        return this.mediumPriorityService.getLargestPoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#setLowPriorityServiceCorePoolSize(int)
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getLowPriorityServiceCorePoolSize()
     */
    @Override
    public int getLowPriorityServiceCorePoolSize(){
        return this.lowPriorityService.getCorePoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getLowPriorityServiceMaximumPoolSize()
     */
    @Override
    public int getLowPriorityServiceMaximumPoolSize(){
        return this.lowPriorityService.getMaximumPoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getLowPriorityServiceActiveCount()
     */
    @Override
    public int getLowPriorityServiceActiveCount(){
        return this.lowPriorityService.getActiveCount();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getLowPriorityServiceLargestPoolSize()
     */
    @Override
    public int getLowPriorityServiceLargestPoolSize(){
        return this.lowPriorityService.getLargestPoolSize();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getLowPriorityServiceQueueSize()
     */
    @Override
    public int getLowPriorityServiceQueueSize() {
        return lowPriorityService.getQueue().size();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getLowPriorityServiceQueueItems()
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#initialize(boolean)
     */
    @Override
    public void initialize(boolean safe) {
        if (state != PRE_INITIALIZE)
            return;

        // Set the started indicator to true.
        state = INITIALIZE;

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
        int corePoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE);
        int maxPoolSize = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE);
        this.highPriorityService.setMaximumPoolSize(maxPoolSize);
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
        this.state = RUNNING;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#terminate()
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#joinTermination()
     */
    @Override
    public void joinTermination() {
        if(state != TERMINATE)
            return;
        state = POST_TERMINATE;
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
                if(highPriorityService.awaitTermination(1, TimeUnit.SECONDS))
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getHighPriorityRejectionHandler()
     */
    @Override
    public TaskRejectionHandler getHighPriorityRejectionHandler(){
        return this.highPriorityRejectionHandler;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getMediumPriorityRejectionHandler()
     */
    @Override
    public TaskRejectionHandler getMediumPriorityRejectionHandler(){
        return this.mediumPriorityRejectionHandler;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.maint.BackroundProcessing#getThreadsList(int)
     */
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
    public class RejectableWorkItemRunnable extends Task{

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
     * @author Terry Packer
     *
     */
    public class WorkItemRunnable implements Runnable{

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
                String message = "Error in work item ";
                if(item.getDescription() != null) {
                    message += item.getDescription();
                }
                if(item.getTaskId() != null) {
                    message += " with id " + item.getTaskId();
                }
                log.error(message, t);
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
