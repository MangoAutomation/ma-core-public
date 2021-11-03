/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.publish.PublishedPointRT;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycle;
import com.serotonin.util.ILifecycleState;


/**
 *
 * @author Terry Packer
 */
public interface RuntimeManager extends ILifecycle {

    /**
     * Check the state of the RuntimeManager
     *  useful if you are a task that may run before/after the RUNNING state
     * @return state
     */
    ILifecycleState getLifecycleState();

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

    /**
     * Get a running data source's runtime object.
     *
     * @param dataSourceId id of the data source
     * @return the data source runtime
     * @throws RTException if the data source is not running
     */
    DataSourceRT<? extends DataSourceVO> getRunningDataSource(int dataSourceId) throws RTException;

    Collection<? extends DataSourceRT<?>> getRunningDataSources();

    boolean isDataSourceRunning(int dataSourceId);

    /**
     * Starts the data source and starts it polling.
     * @param vo The data source VO
     * @throws IllegalArgumentException if the data source is not saved, or is not enabled
     * @throws IllegalStateException if the data source is already running or terminated
     */
    default void startDataSource(DataSourceVO vo) {
        startDataSource(vo, true);
    }

    /**
     * Starts the data source.
     * @param vo The data source VO
     * @throws IllegalArgumentException if the data source is not saved, or is not enabled
     * @throws IllegalStateException if the data source is already running or terminated
     */
    void startDataSource(DataSourceVO vo, boolean startPolling);

    /**
     * Stops the data source (if running)
     * @param dataSourceId the data source id
     */
    void stopDataSource(int dataSourceId);

    //
    //
    // Data points
    //

    /**
     * Starts a data point with a null initial cache.
     * @param vo data point with its event detectors
     */
    default void startDataPoint(DataPointWithEventDetectors vo) {
        startDataPoint(vo, null);
    }

    /**
     * Start a data point, will confirm that it is enabled
     * @param vo data point with its event detectors
     * @param initialCache if null then data point will retrieve from database when it is initialized
     */
    void startDataPoint(DataPointWithEventDetectors vo, @Nullable List<PointValueTime> initialCache);

    /**
     * Stop a running data point
     * @param dataPointId id of data point
     */
    void stopDataPoint(int dataPointId);

    /**
     * Check if a data point is running
     * @param dataPointId id of data point
     * @return true if running
     */
    boolean isDataPointRunning(int dataPointId);

    /**
     * Removes a point from the running points list. Must be terminated.
     * @param dataPoint data point to remove
     */
    void removeDataPoint(DataPointRT dataPoint);

    /**
     * Removes a data source from the running data sources list. Must be terminated.
     * @param dataSource data source to remove
     */
    void removeDataSource(DataSourceRT<? extends DataSourceVO> dataSource);

    /**
     * Get the RT of a running data point, can be null if not running
     * @param dataPointId id of data point
     * @return the runtime for the data point
     */
    @Nullable
    DataPointRT getDataPoint(int dataPointId);

    Collection<DataPointRT> getRunningDataPoints();

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
     * @param dataPointId id of data point
     */
    void forcePointRead(int dataPointId);

    void forceDataSourcePoll(int dataSourceId);

    Optional<Long> purgeDataPointValues(DataPointVO vo, int periodType, int periodCount);

    Optional<Long> purgeDataPointValues(DataPointVO vo);

    /**
     * Purge a value at a given time
     * @param vo data point VO
     * @param ts epoch timestamp in ms
     * @return number of point values purged
     */
    Optional<Long> purgeDataPointValue(DataPointVO vo, long ts);

    /**
     * Purge values before a given time
     * @param vo data point VO
     * @param before epoch timestamp in ms
     * @return count of values deleted
     */
    Optional<Long> purgeDataPointValues(DataPointVO vo, long before);

    /**
     * Purge values between a time range inclusive of startTime exclusive of endTime
     * @param vo data point VO
     * @param startTime epoch timestamp in ms
     * @param endTime epoch timestamp in ms
     * @return count of values deleted
     */
    Optional<Long> purgeDataPointValuesBetween(DataPointVO vo, long startTime, long endTime);

    //
    //
    // Publishers
    //
    Collection<PublisherRT<? extends PublisherVO, ? extends PublishedPointVO>> getRunningPublishers();

    @Nullable
    PublisherRT<? extends PublisherVO, ? extends PublishedPointVO> getRunningPublisher(int publisherId);

    boolean isPublisherRunning(int publisherId);

    /**
     * Starts the publisher
     * @param vo the publisher VO
     * @throws IllegalArgumentException if the publisher is not saved, or is not enabled
     */
    void startPublisher(PublisherVO vo);

    /**
     * Stops the publisher
     * @param publisherId id of the publisher
     */
    void stopPublisher(int publisherId);

    /**
     * Removes a publisher from the running publishers list. Must be terminated.
     * @param publisher publisher to remove
     */
    void removePublisher(PublisherRT<? extends PublisherVO, ? extends PublishedPointVO> publisher);

    /**
     * Start a published point, will confirm that it is enabled
     * @param vo published point
     */
    void startPublishedPoint(PublishedPointVO vo);

    /**
     * Stop a running published point
     * @param id of published point
     */
    void stopPublishedPoint(int id);

    /**
     * Stop all running published points for a given data point.  Used
     * when a data point is deleted
     * @param dataPointId of source data point
     */
    void stopPublishedPointsForDataPoint(int dataPointId);

    /**
     * Check if a published point is running
     * @param id id of published point
     * @return true if running
     */
    boolean isPublishedPointRunning(int id);

    /**
     * Get the RT of a running published point, can be null if not running
     * @param id of published point
     * @return the runtime for the published point
     */
    @Nullable
    PublishedPointRT<? extends PublishedPointVO> getPublishedPoint(int id);

    /**
     * Removes a point from the running published points list. Must be terminated.
     * @param publishedPoint point to remove
     */
    void removePublishedPoint(PublishedPointRT<? extends PublishedPointVO> publishedPoint);

    /**
     * Get a message about what state we are in
     * @return message indicating the runtime state
     */
    TranslatableMessage getStateMessage();

}
