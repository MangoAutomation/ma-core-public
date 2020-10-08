/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * This class is used at startup to initialize data points on a single source in parallel.
 *
 * @author Terry Packer
 */
public class DataPointGroupInitializer {

    private final Log LOG = LogFactory.getLog(DataPointGroupInitializer.class);

    private List<DataPointWithEventDetectorsAndCache> group;
    private int threadPoolSize;
    private List<DataPointSubGroupInitializer> runningTasks;
    private boolean useMetrics;

    /**
     *
     * @param group
     * @param logMetrics
     * @param threadPoolSize
     */
    public DataPointGroupInitializer(List<DataPointWithEventDetectorsAndCache> group, boolean logMetrics, int threadPoolSize) {
        this.group = group;
        this.useMetrics = logMetrics;
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Blocking method that will attempt to start all datasources in parallel using threadPoolSize number of threads at most.
     * @return List of all data sources that need to begin polling.
     */
    public void initialize() {

        long startTs = Common.timer.currentTimeMillis();
        if(this.group == null){
            if(this.useMetrics)
                LOG.info("Initialization of 0 data points took " + (Common.timer.currentTimeMillis() - startTs));
            return;
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
            LOG.info("Initializing " + this.group.size() + " data points in " + this.threadPoolSize + " threads.");

        this.runningTasks = new ArrayList<DataPointSubGroupInitializer>(this.threadPoolSize);
        //Add and Start the tasks
        int endPos;
        for(int i=0; i<this.threadPoolSize; i++){

            if(i==lastGroup){
                //Last group may be larger
                endPos = this.group.size();
            }else{
                endPos = (i*subGroupSize) + subGroupSize;
            }

            DataPointSubGroupInitializer currentSubgroup = new DataPointSubGroupInitializer(this.group.subList(i*subGroupSize, endPos), this);

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
            LOG.info("Initialization of " + this.group.size() + " data points took " + (Common.timer.currentTimeMillis() - startTs) + "ms");

        return;
    }

    public void removeRunningTask(DataPointSubGroupInitializer task){
        synchronized(this.runningTasks){
            this.runningTasks.remove(task);
        }
    }

    /**
     * Initialize a sub group of the data sources in one thread.
     * @author Terry Packer
     *
     */
    class DataPointSubGroupInitializer extends HighPriorityTask{

        private final Log LOG = LogFactory.getLog(DataPointSubGroupInitializer.class);

        private List<DataPointWithEventDetectorsAndCache> subgroup;
        private DataPointGroupInitializer parent;

        public DataPointSubGroupInitializer(List<DataPointWithEventDetectorsAndCache> subgroup, DataPointGroupInitializer parent){
            super("Data point subgroup initializer");
            this.subgroup = subgroup;
            this.parent = parent;
        }

        @Override
        public void run(long runtime) {
            try{
                for(DataPointWithEventDetectorsAndCache config : subgroup){
                    try{
                        Common.runtimeManager.startDataPointStartup(config);
                    }catch(Exception e){
                        //Ensure only 1 can fail at a time
                        LOG.error(e.getMessage(), e);
                    }
                }
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

    public static class DataPointWithEventDetectorsAndCache extends DataPointWithEventDetectors {
        private final List<PointValueTime> initialCache;

        public DataPointWithEventDetectorsAndCache(DataPointWithEventDetectors vo, List<PointValueTime> initialCache) {
            super(vo.getDataPoint(), vo.getEventDetectors());
            this.initialCache = initialCache;
        }

        public DataPointWithEventDetectorsAndCache(DataPointVO vo,
                List<AbstractPointEventDetectorVO> detectors, List<PointValueTime> initialCache) {
            super(vo, detectors);
            this.initialCache = initialCache;
        }

        public List<PointValueTime> getInitialCache() {
            return initialCache;
        }
    }
}
