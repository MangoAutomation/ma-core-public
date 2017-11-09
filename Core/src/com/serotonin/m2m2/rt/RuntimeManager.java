/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt;

import java.util.List;

import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycle;


/**
 *
 * @author Terry Packer
 */
public interface RuntimeManager extends ILifecycle{

    /**
     * Check the state of the RuntimeManager
     *  useful if you are a task that may run before/after the RUNNING state
     * @return
     */
    int getState();

    //
    // Lifecycle
    void initialize(boolean safe);

    void terminate();

    void joinTermination();

    //
    //
    // Data sources
    //
    DataSourceRT<? extends DataSourceVO<?>> getRunningDataSource(int dataSourceId);

    boolean isDataSourceRunning(int dataSourceId);

    List<DataSourceVO<?>> getDataSources();

    DataSourceVO<?> getDataSource(int dataSourceId);

    void deleteDataSource(int dataSourceId);

    void saveDataSource(DataSourceVO<?> vo);
    
    /**
     * Initialize a data source (only to be used at system startup)
     * @param vo
     * @return
     */
    boolean initializeDataSourceStartup(DataSourceVO<?> vo);
    
    /**
     * Stop a data source (only to be used at system shutdown)
     * @param id
     */
    void stopDataSourceShutdown(int id);

    //
    //
    // Data points
    //
    void saveDataPoint(DataPointVO point);
    
    void enableDataPoint(DataPointVO point, boolean enabled);

    void deleteDataPoint(DataPointVO point);

    void restartDataPoint(DataPointVO vo);

    boolean isDataPointRunning(int dataPointId);

    DataPointRT getDataPoint(int dataPointId);

    void addDataPointListener(int dataPointId, DataPointListener l);

    void removeDataPointListener(int dataPointId, DataPointListener l);

    DataPointListener getDataPointListeners(int dataPointId);

    //
    // Point values
    void setDataPointValue(int dataPointId, DataValue value, SetPointSource source);

    void setDataPointValue(int dataPointId, PointValueTime valueTime, SetPointSource source);

    void relinquish(int dataPointId);

    /**
     * This method forces a point read ONLY if the 
     * underlying data source has implemented that ability.
     * 
     * Currently only a few data sources implement this functionality
     * @param dataPointId
     */
    void forcePointRead(int dataPointId);

    void forceDataSourcePoll(int dataSourceId);

    long purgeDataPointValues();

    void purgeDataPointValuesWithoutCount();

    long purgeDataPointValues(int dataPointId, int periodType, int periodCount);

    long purgeDataPointValues(int dataPointId);

    /**
     * @param id
     */
    boolean purgeDataPointValuesWithoutCount(int dataPointId);

    /**
     * Purge a value at a given time
     * @param dataPointId
     * @param ts
     * @return
     */
    long purgeDataPointValue(int dataPointId, long ts);

    long purgeDataPointValues(int dataPointId, long before);

    long purgeDataPointValuesBetween(int dataPointId, long startTime, long endTime);

    /**
     * Purge values before a given time
     * @param dataPointId
     * @param before
     * @return true if any data was deleted
     */
    boolean purgeDataPointValuesWithoutCount(int dataPointId, long before);

    //
    //
    // Publishers
    //
    PublisherRT<?> getRunningPublisher(int publisherId);

    boolean isPublisherRunning(int publisherId);

    PublisherVO<? extends PublishedPointVO> getPublisher(int publisherId);

    void deletePublisher(int publisherId);

    void savePublisher(PublisherVO<? extends PublishedPointVO> vo);

}