/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DataSourceDefinition.StartPriority;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * This class is used at startup to initialize data sources in parallel.
 *
 * The group is generally a list of all data sources with the same priority level.
 * The group is not initalized until all data sources have either started or failed to start.
 *
 * @author Terry Packer
 *
 */
public class DataSourceGroupInitializer {
    private final Log LOG = LogFactory.getLog(DataSourceGroupInitializer.class);

    private List<DataSourceVO> group;
    private int threadPoolSize;
    private List<DataSourceSubGroupInitializer> runningTasks;
    private List<DataSourceVO> polling;
    private boolean useMetrics;
    private StartPriority startPriority;

    /**
     *
     * @param group
     * @param logMetrics
     * @param threadPoolSize
     */
    public DataSourceGroupInitializer(StartPriority startPriority, List<DataSourceVO> group, boolean logMetrics, int threadPoolSize) {
        this.startPriority = startPriority;
        this.group = group;
        this.useMetrics = logMetrics;
        this.threadPoolSize = threadPoolSize;
        this.polling = new ArrayList<DataSourceVO>();
    }

    /**
     * Blocking method that will attempt to start all datasources in parallel using threadPoolSize number of threads at most.
     * @return List of all data sources that need to begin polling.
     */
    public List<DataSourceVO> initialize() {

        long startTs = Common.timer.currentTimeMillis();
        if(this.group == null){
            if(this.useMetrics)
                LOG.info("Initialization of 0 " + this.startPriority.name() +  " priority data sources took " + (Common.timer.currentTimeMillis() - startTs));
            return polling;
        }

        //Compute the size of the subGroup that each thread will use.
        int subGroupSize = this.group.size() / this.threadPoolSize;
        int lastGroup;
        if(subGroupSize == 0){
            subGroupSize = 1;
            lastGroup = this.group.size() - 1;
        }else{
            lastGroup = this.threadPoolSize - 1;
        }


        if(useMetrics)
            LOG.info("Initializing " + this.group.size() + " " + this.startPriority.name() + " priority data sources in " + this.threadPoolSize + " threads.");

        this.runningTasks = new ArrayList<DataSourceSubGroupInitializer>(this.threadPoolSize);
        //Add and Start the tasks
        int endPos;
        for(int i=0; i<this.threadPoolSize; i++){

            if(i==lastGroup){
                //Last group may be larger
                endPos = this.group.size();
            }else{
                endPos = (i*subGroupSize) + subGroupSize;
            }

            DataSourceSubGroupInitializer currentSubgroup = new DataSourceSubGroupInitializer(this.group.subList(i*subGroupSize, endPos), this);

            synchronized(this.runningTasks){
                this.runningTasks.add(currentSubgroup);
            }
            Common.backgroundProcessing.execute(currentSubgroup);

            //When we have more threads than groups
            if(i >= this.group.size() -1)
                break;
        }

        //Wait here until all threads are finished
        while(runningTasks.size() > 0){
            try { Thread.sleep(100); } catch (InterruptedException e) { }
        }

        if(this.useMetrics)
            LOG.info("Initialization of " + this.group.size() + " " + this.startPriority.name() +  " priority data sources took " + (Common.timer.currentTimeMillis() - startTs) + "ms");

        return polling;
    }

    public void addPollingDataSources(List<DataSourceVO> vos){
        synchronized(this.polling){
            this.polling.addAll(vos);
        }
    }

    public void removeRunningTask(DataSourceSubGroupInitializer task){
        synchronized(this.runningTasks){
            this.runningTasks.remove(task);
        }
    }
    /**
     * Initialize a sub group of the data sources in one thread.
     * @author Terry Packer
     *
     */
    class DataSourceSubGroupInitializer extends HighPriorityTask{

        private final Log LOG = LogFactory.getLog(DataSourceSubGroupInitializer.class);

        private List<DataSourceVO> subgroup;
        private DataSourceGroupInitializer parent;

        public DataSourceSubGroupInitializer(List<DataSourceVO> subgroup, DataSourceGroupInitializer parent){
            super("Datasource subgroup initializer");
            this.subgroup = subgroup;
            this.parent = parent;
        }

        @Override
        public void run(long runtime) {
            try{
                List<DataSourceVO> polling = new ArrayList<DataSourceVO>();
                for(DataSourceVO config : subgroup){
                    try{
                        if(Common.runtimeManager.initializeDataSourceStartup(config))
                            polling.add(config);
                    }catch(Exception e){
                        //Ensure only 1 can fail at a time
                        LOG.error(e.getMessage(), e);
                    }
                }
                this.parent.addPollingDataSources(polling);
            }catch(Exception e){
                LOG.error(e.getMessage(), e);
            }finally{
                this.parent.removeRunningTask(this);
            }
        }

        @Override
        public boolean cancel() {
            this.parent.removeRunningTask(this);
            return super.cancel();
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            this.parent.removeRunningTask(this);
            super.rejected(reason);
        }
    }

}
