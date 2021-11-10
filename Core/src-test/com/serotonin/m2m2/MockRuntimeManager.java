/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.RuntimeManagerDefinition;
import com.serotonin.m2m2.rt.RTException;
import com.serotonin.m2m2.rt.RuntimeManager;
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
import com.serotonin.util.ILifecycleState;

/**
 * A mock of the runtime manager that can optionally save data to the database.
 *   This implementation does not actually start any threads/tasks.
 *
 * @author Terry Packer
 */
public class MockRuntimeManager implements RuntimeManager {

    //Use the database to save data sources/points/publishers
    protected boolean useDatabase;

    public MockRuntimeManager() {

    }

    public MockRuntimeManager(boolean useDatabase) {
        this.useDatabase = useDatabase;
    }

    @Override
    public ILifecycleState getLifecycleState() {
        return ILifecycleState.RUNNING;
    }

    @Override
    public void initialize(boolean safe) {
        //Get the RTM defs from modules and sort by priority
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        Collections.sort(defs, new Comparator<RuntimeManagerDefinition>() {
            @Override
            public int compare(RuntimeManagerDefinition def1, RuntimeManagerDefinition def2) {
                return def1.getInitializationPriority() - def2.getInitializationPriority();
            }
        });

        //Initialize them
        defs.stream().forEach((def) -> {
            def.initialize(safe);
        });
    }

    @Override
    public void terminate() {
        // Get the RTM defs and sort by reverse init priority.
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        Collections.sort(defs, new Comparator<RuntimeManagerDefinition>() {
            @Override
            public int compare(RuntimeManagerDefinition def1, RuntimeManagerDefinition def2) {
                return def2.getInitializationPriority() - def1.getInitializationPriority();
            }
        });
        //Initialize them
        defs.stream().forEach((def) -> {
            def.terminate();
        });
    }

    @Override
    public void joinTermination() {

    }

    @Override
    public DataSourceRT<? extends DataSourceVO> getRunningDataSource(int dataSourceId) {
        throw new RTException("Data source is not running");
    }

    @Override
    public Collection<? extends DataSourceRT<?>> getRunningDataSources() {
        return new ArrayList<>();
    }

    @Override
    public boolean isDataSourceRunning(int dataSourceId) {
        return false;
    }

    @Override
    public void startDataSource(DataSourceVO vo, boolean startPolling) {
    }

    @Override
    public void stopDataSource(int dataSourceId) {

    }

    @Override
    public boolean isDataPointRunning(int dataPointId) {

        return false;
    }

    @Override
    public void removeDataPoint(DataPointRT dataPoint) {

    }

    @Override
    public void removeDataSource(DataSourceRT<? extends DataSourceVO> dataSource) {

    }

    @Override
    public DataPointRT getDataPoint(int dataPointId) {
        return null;
    }

    @Override
    public Collection<DataPointRT> getRunningDataPoints() {
        return new ArrayList<>();
    }

    @Override
    public void addDataPointListener(int dataPointId, DataPointListener l) {

    }

    @Override
    public void removeDataPointListener(int dataPointId, DataPointListener l) {

    }

    @Override
    public DataPointListener getDataPointListeners(int dataPointId) {

        return null;
    }

    @Override
    public void setDataPointValue(int dataPointId, DataValue value, SetPointSource source) {


    }

    @Override
    public void setDataPointValue(int dataPointId, PointValueTime valueTime,
            SetPointSource source) {


    }

    @Override
    public void relinquish(int dataPointId) {


    }

    @Override
    public void forcePointRead(int dataPointId) {


    }

    @Override
    public void forceDataSourcePoll(int dataSourceId) {


    }

    @Override
    public Optional<Long> purgeDataPointValues(DataPointVO vo, int periodType, int periodCount) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> purgeDataPointValues(DataPointVO vo) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> purgeDataPointValue(DataPointVO vo, long ts) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> purgeDataPointValues(DataPointVO vo, long before) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> purgeDataPointValuesBetween(DataPointVO vo, long startTime, long endTime) {
        return Optional.empty();
    }

    @Override
    public PublisherRT<?,?> getRunningPublisher(int publisherId) {

        return null;
    }

    @Override
    public boolean isPublisherRunning(int publisherId) {

        return false;
    }

    @Override
    public void startPublisher(PublisherVO vo) {

    }

    @Override
    public void stopPublisher(int publisherId) {

    }

    @Override
    public void removePublisher(PublisherRT<? extends PublisherVO, ? extends PublishedPointVO> publisher) {

    }

    @Override
    public void startPublishedPoint(PublishedPointVO vo) {

    }

    @Override
    public void stopPublishedPoint(int id) {

    }

    @Override
    public boolean isPublishedPointRunning(int id) {
        return false;
    }

    @Override
    public @Nullable PublishedPointRT<? extends PublishedPointVO> getPublishedPoint(int id) {
        return null;
    }

    @Override
    public void removePublishedPoint(PublishedPointRT<? extends PublishedPointVO> publishedPoint) {

    }

    @Override
    public TranslatableMessage getStateMessage() {
        return new TranslatableMessage("common.default", "Mock runtime manager state message");
    }

    @Override
    public Collection<PublisherRT<? extends PublisherVO, ? extends PublishedPointVO>> getRunningPublishers() {
        if(useDatabase) {
            List<PublisherRT<?,?>> running = new ArrayList<>();
            for(PublisherVO vo : PublisherDao.getInstance().getAll()) {
                if(vo.isEnabled()) {
                    running.add(vo.createPublisherRT());
                }
            }
            return running;
        }else
            return null;
    }

    @Override
    public void startDataPoint(DataPointWithEventDetectors vo) {
        if(useDatabase) {
            vo.getDataPoint().setEnabled(true);
            DataPointDao.getInstance().saveEnabledColumn(vo.getDataPoint());
        }
    }

    @Override
    public void stopDataPoint(int id) {
        if(useDatabase) {
            DataPointVO vo = DataPointDao.getInstance().get(id);
            vo.setEnabled(false);
            DataPointDao.getInstance().saveEnabledColumn(vo);
        }
    }

    @Override
    public void startDataPoint(DataPointWithEventDetectors vo, @Nullable List<PointValueTime> initialCache) {
        if (useDatabase) {
            vo.getDataPoint().setEnabled(true);
            DataPointDao.getInstance().saveEnabledColumn(vo.getDataPoint());
        }
    }
}
