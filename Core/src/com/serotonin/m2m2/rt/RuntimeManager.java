/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt;

import java.util.List;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycle;


/**
 *
 * @author Terry Packer
 */
public interface RuntimeManager extends ILifecycle {

    /**
     * Check the state of the RuntimeManager
     *  useful if you are a task that may run before/after the RUNNING state
     * @return
     */
    int getState();

    //
    // Lifecycle
    @Override
    void initialize(boolean safe);

    @Override
    void terminate();

    @Override
    void joinTermination();

    //
    //
    // Data sources
    //
    DataSourceRT<? extends DataSourceVO> getRunningDataSource(int dataSourceId);

    List<? extends DataSourceRT<?>> getRunningDataSources();

    boolean isDataSourceRunning(int dataSourceId);

    DataSourceVO getDataSource(int dataSourceId);

    void deleteDataSource(int dataSourceId);

    /**
     * Insert a new data source
     * @param vo
     */
    void insertDataSource(DataSourceVO vo);

    /**
     * Update a data source
     * @param existing
     * @param vo
     */
    void updateDataSource(DataSourceVO existing, DataSourceVO vo);

    /**
     * Initialize a data source (only to be used at system startup)
     * @param vo
     * @return
     */
    boolean initializeDataSourceStartup(DataSourceVO vo);

    /**
     * Stop a data source (only to be used at system shutdown)
     * @param id
     */
    void stopDataSourceShutdown(int id);

    //
    //
    // Data points
    //
    /**
     * Start a data point, will confirm that it is enabled
     * @param vo
     */
    void startDataPoint(DataPointWithEventDetectors vo);

    /**
     * Only to be used at startup as synchronization has been reduced for performance
     * @param vo
     */
    void startDataPointStartup(DataPointWithEventDetectorsAndCache vo);

    /**
     *
     * @param id
     */
    void stopDataPoint(int id);

    /**
     * Is this data point running?
     * @param dataPointId
     * @return
     */
    boolean isDataPointRunning(int dataPointId);

    /**
     * Get the RT of a running data point, can be null if not running
     * @param dataPointId
     * @return
     */
    DataPointRT getDataPoint(int dataPointId);

    List<DataPointRT> getRunningDataPoints();

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

    long purgeDataPointValues(DataPointVO vo, int periodType, int periodCount);

    long purgeDataPointValues(DataPointVO vo);

    /**
     * @param id
     */
    boolean purgeDataPointValuesWithoutCount(DataPointVO vo);

    /**
     * Purge a value at a given time
     * @param dataPointId
     * @param ts
     * @param dao to aid in performance of high frequency calls
     * @return
     */
    long purgeDataPointValue(DataPointVO vo, long ts, PointValueDao dao);

    long purgeDataPointValues(DataPointVO vo, long before);

    long purgeDataPointValuesBetween(DataPointVO vo, long startTime, long endTime);

    /**
     * Purge values before a given time
     * @param dataPointId
     * @param before
     * @return true if any data was deleted
     */
    boolean purgeDataPointValuesWithoutCount(DataPointVO vo, long before);

    //
    //
    // Publishers
    //
    List<PublisherRT<?>> getRunningPublishers();

    PublisherRT<?> getRunningPublisher(int publisherId);

    boolean isPublisherRunning(int publisherId);

    PublisherVO<? extends PublishedPointVO> getPublisher(int publisherId);

    void deletePublisher(int publisherId);

    void savePublisher(PublisherVO<? extends PublishedPointVO> vo);

    /**
     * Get a message about what state we are in
     * @return
     */
    TranslatableMessage getStateMessage();

}