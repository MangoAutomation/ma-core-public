/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition.StartPriority;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.RuntimeManagerDefinition;
import com.serotonin.m2m2.rt.dataImage.DataPointEventMulticaster;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycleState;

public class RuntimeManagerImpl implements RuntimeManager {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeManagerImpl.class);

    private final ConcurrentMap<Integer, DataSourceRT<? extends DataSourceVO>> runningDataSources = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, DataPointRT> dataPointCache  = new ConcurrentHashMap<>();

    /**
     * The list of point listeners, kept here such that listeners can be notified of point initializations (i.e. a
     * listener can register itself before the point is enabled).
     */
    private final ConcurrentMap<Integer, DataPointListener> dataPointListeners = new ConcurrentHashMap<>();

    /**
     * Store of enabled publishers
     */
    private final ConcurrentMap<Integer, PublisherRT<? extends PublishedPointVO>> runningPublishers = new ConcurrentHashMap<>();

    private final ExecutorService executorService;
    private final DataSourceDao dataSourceDao;
    private final PublisherDao publisherDao;
    private final DataPointDao dataPointDao;

    private TranslatableMessage stateMessage = new TranslatableMessage("startup.state.runtimeManagerInitialize");

    /**
     * State machine allowed order:
     * PRE_INITIALIZE
     * INITIALIZE
     * RUNNING
     * TERMINATE
     * POST_TERMINATE
     * TERMINATED
     *
     */
    private ILifecycleState state = ILifecycleState.PRE_INITIALIZE;

    public RuntimeManagerImpl(ExecutorService executorService, DataSourceDao dataSourceDao, PublisherDao publisherDao, DataPointDao dataPointDao) {
        this.executorService = executorService;
        this.dataSourceDao = dataSourceDao;
        this.publisherDao = publisherDao;
        this.dataPointDao = dataPointDao;
    }

    @Override
    public ILifecycleState getLifecycleState(){
        return state;
    }

    @Override
    public TranslatableMessage getStateMessage() {
        switch(state) {
            case PRE_INITIALIZE:
            case INITIALIZING:
                return stateMessage;
            case RUNNING:
                return new TranslatableMessage("startup.state.running");
            case TERMINATING:
            case TERMINATED:
            default:
                return new TranslatableMessage("shutdown.state.preTerminate");
        }
    }

    //
    // Lifecycle

    @Override
    synchronized public void initialize(boolean safe) {
        ensureState(ILifecycleState.PRE_INITIALIZE);

        // Set the started indicator to true.
        state = ILifecycleState.INITIALIZING;

        //Get the RTM defs from modules
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        defs.sort(Comparator.comparingInt(RuntimeManagerDefinition::getInitializationPriority));

        // Start everything with priority up to and including 4.
        int rtmdIndex = startRTMDefs(defs, safe, 0, 4);

        // Initialize data sources that are enabled. Start by organizing all enabled data sources by start priority.
        List<DataSourceVO> configs = dataSourceDao.getAll();
        Map<StartPriority, List<DataSourceVO>> priorityMap = new HashMap<>();
        for (DataSourceVO config : configs) {
            if (config.isEnabled()) {
                if (safe) {
                    config.setEnabled(false);
                    dataSourceDao.update(config.getId(), config);
                }
                else if (config.getDefinition() != null) {
                    List<DataSourceVO> priorityList = priorityMap.computeIfAbsent(config.getDefinition().getStartPriority(), k -> new ArrayList<>());
                    priorityList.add(config);
                }
            }
        }

        // Initialize the prioritized data sources. Start the polling later.
        List<DataSourceVO> pollingRound = new ArrayList<>();
        int startupThreads = Common.envProps.getInt("runtime.datasource.startupThreads", 1);
        for (StartPriority startPriority : StartPriority.values()) {
            List<DataSourceVO> priorityList = priorityMap.get(startPriority);
            if (priorityList != null) {
                DataSourceGroupInitializer initializer = new DataSourceGroupInitializer(
                        executorService, startupThreads, startPriority);
                pollingRound.addAll(initializer.process(priorityList));
            }
        }

        // Tell the data sources to start polling. Delaying the polling start gives the data points a chance to
        // initialize such that point listeners in meta points and set point handlers can run properly.
        for (DataSourceVO config : pollingRound) {
            DataSourceRT<? extends DataSourceVO> dataSource = getRunningDataSource(config.getId());
            dataSource.beginPolling();
        }

        // Run everything else.
        startRTMDefs(defs, safe, rtmdIndex, Integer.MAX_VALUE);

        // Start the publishers that are enabled
        long pubStart = Common.timer.currentTimeMillis();
        List<PublisherVO<? extends PublishedPointVO>> publishers = publisherDao.getAll();
        //Sort into a list of only enabled publishers
        List<PublisherVO<? extends PublishedPointVO>> publishersToStart = new ArrayList<>();
        for (PublisherVO<? extends PublishedPointVO> vo : publishers) {
            if (vo.isEnabled()) {
               publishersToStart.add(vo);
            }
        }
        LOG.info("Starting " + publishersToStart.size() + " Publishers...");
        for (PublisherVO<? extends PublishedPointVO> vo : publishersToStart) {
                if (safe) {
                    vo.setEnabled(false);
                    publisherDao.update(vo.getId(), vo);
                }
                else {
                    LOG.info("Starting publisher: " + vo.getName());
                    startPublisher(vo);
                }
        }
        LOG.info(publishersToStart.size() + " Publisher's started in " +  (Common.timer.currentTimeMillis() - pubStart) + "ms");

        //Schedule the Backup Tasks if necessary
        if(!safe){
            if(SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.BACKUP_ENABLED)){
                BackupWorkItem.schedule();
            }
            if(SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.DATABASE_BACKUP_ENABLED)){
                DatabaseBackupWorkItem.schedule();
            }

        }
        //This is a bit of a misnomer since we startup the data sources in separate threads and don't callback when running.
        this.state = ILifecycleState.RUNNING;
    }

    @Override
    synchronized public void terminate() {
        ensureState(ILifecycleState.RUNNING);
        state = ILifecycleState.TERMINATING;

        for (PublisherRT<? extends PublishedPointVO> publisher : runningPublishers.values())
            stopPublisher(publisher.getId());

        // Get the RTM defs and sort by reverse init priority.
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        defs.sort((def1, def2) -> def2.getInitializationPriority() - def1.getInitializationPriority());

        // Stop everything with priority up to and including 5.
        int rtmdIndex = stopRTMDefs(defs, 0, 5);

        // Stop data sources in reverse start priority order.
        Map<StartPriority, List<DataSourceRT<? extends DataSourceVO>>> priorityMap = new HashMap<>();
        for (Entry<Integer, DataSourceRT<? extends DataSourceVO>> entry : runningDataSources.entrySet()) {
            DataSourceRT<? extends DataSourceVO> rt = entry.getValue();
            List<DataSourceRT<? extends DataSourceVO>> priorityList = priorityMap.computeIfAbsent(rt.getVo().getDefinition().getStartPriority(), k -> new ArrayList<>());
            priorityList.add(rt);
        }

        int dataSourceShutdownThreads = Common.envProps.getInt("runtime.datasource.shutdownThreads", 1);
        StartPriority[] priorities = StartPriority.values();
        for (int i = priorities.length - 1; i >= 0; i--) {
            List<DataSourceRT<? extends DataSourceVO>> priorityList = priorityMap.get(priorities[i]);
            if (priorityList != null) {
                DataSourceGroupTerminator initializer = new DataSourceGroupTerminator(
                        executorService, dataSourceShutdownThreads, priorities[i]);
                initializer.process(priorityList);
            }
        }

        // Run everything else.
        stopRTMDefs(defs, rtmdIndex, Integer.MIN_VALUE);
    }

    @Override
    public void joinTermination() {
        if (state == ILifecycleState.TERMINATED) return;
        ensureState(ILifecycleState.TERMINATING);

        for (Entry<Integer, DataSourceRT<? extends DataSourceVO>> entry : runningDataSources.entrySet()) {
            DataSourceRT<? extends DataSourceVO> dataSource = entry.getValue();
            try {
                dataSource.joinTermination();
            }
            catch (ShouldNeverHappenException e) {
                LOG.error("Error stopping data source " + dataSource.getId(), e);
            }
        }
        state = ILifecycleState.TERMINATED;
    }

    private int startRTMDefs(List<RuntimeManagerDefinition> defs, boolean safe, int fromIndex, int toPriority) {
        while (fromIndex < defs.size() && defs.get(fromIndex).getInitializationPriority() <= toPriority) {
            defs.get(fromIndex).initialize(safe);
            stateMessage = new TranslatableMessage("runtimeManager.initialize.rtm", defs.get(fromIndex).getModule().getName());
            fromIndex++;
        }
        return fromIndex;
    }

    private int stopRTMDefs(List<RuntimeManagerDefinition> defs, int fromIndex, int toPriority) {
        while (fromIndex < defs.size() && defs.get(fromIndex).getInitializationPriority() >= toPriority)
            defs.get(fromIndex++).terminate();
        return fromIndex;
    }

    //
    //
    // Data sources
    //
    @Override
    public @NonNull DataSourceRT<? extends DataSourceVO> getRunningDataSource(int dataSourceId) throws RTException {
        DataSourceRT<? extends DataSourceVO> ds = runningDataSources.get(dataSourceId);
        if (ds == null) {
            throw new RTException(String.format("Data source is not running: id=%d, type=%s",
                    dataSourceId, getClass()));
        }
        return ds;
    }

    @Override
    public Collection<? extends DataSourceRT<?>> getRunningDataSources() {
        return Collections.unmodifiableCollection(runningDataSources.values());
    }

    @Override
    public boolean isDataSourceRunning(int dataSourceId) {
        DataSourceRT<? extends DataSourceVO> ds = runningDataSources.get(dataSourceId);
        return ds != null && ds.getLifecycleState() == ILifecycleState.RUNNING;
    }

    @Override
    public void startDataSource(DataSourceVO vo, boolean beginPolling) {
        // Ensure that the data source is saved and enabled.
        Assert.isTrue(vo.getId() > 0, "Data source must be saved");
        Assert.isTrue(vo.isEnabled(), "Data source must be enabled");
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);

        DataSourceRT<? extends DataSourceVO> dataSource = runningDataSources.computeIfAbsent(vo.getId(), k -> vo.createDataSourceRT());

        long startTime = System.nanoTime();

        // Create and initialize the runtime version of the data source.
        dataSource.initialize(false);

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        stateMessage = new TranslatableMessage("runtimeManager.initialize.dataSource", vo.getName(), duration);
        LOG.info(String.format("%s took %dms to start", dataSource.readableIdentifier(), duration));

        if (beginPolling) {
            dataSource.beginPolling();
        }
    }

    @Override
    public void stopDataSource(int dataSourceId) {
        DataSourceRT<? extends DataSourceVO> dataSource = runningDataSources.get(dataSourceId);
        if (dataSource != null) {
            long startTime = System.nanoTime();

            try {
                dataSource.terminate();
            } catch (Exception e) {
                LOG.error("Error while terminating " + dataSource.readableIdentifier(), e);
            }

            try {
                dataSource.joinTermination();
            } catch (Exception e) {
                LOG.error("Error while waiting for " + dataSource.readableIdentifier() + " to terminate", e);
            }

            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            LOG.info(String.format("%s stopped in %dms", dataSource.readableIdentifier(), duration));
        }
    }

    //
    //
    // Data points
    //

    @Override
    public void stopDataPoint(int id) {
        DataPointRT point = dataPointCache.get(id);
        if (point != null) {
            point.terminate();
            point.joinTermination();
        }
    }

    @Override
    public void startDataPoint(DataPointWithEventDetectors vo, @Nullable List<PointValueTime> initialCache) {
        DataPointVO dataPointVO = vo.getDataPoint();
        // Ensure that the data point is saved and enabled.
        Assert.isTrue(dataPointVO.getId() > 0, "Data point must be saved");
        Assert.isTrue(dataPointVO.isEnabled(), "Data point not enabled");
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);

        // Only add the data point if its data source is enabled.
        DataSourceRT<? extends DataSourceVO> ds = runningDataSources.get(dataPointVO.getDataSourceId());
        if (ds != null) {
            DataPointRT dataPoint = dataPointCache.computeIfAbsent(dataPointVO.getId(), k -> new DataPointRT(
                    vo,
                    dataPointVO.getPointLocator().createRuntime(),
                    ds,
                    initialCache,
                    Common.databaseProxy.newPointValueDao(),
                    Common.databaseProxy.getPointValueCacheDao()
            ));

            // Initialize it, will fail if data point is already initializing or running
            dataPoint.initialize(false);
        }
    }

    @Override
    public boolean isDataPointRunning(int dataPointId) {
        DataPointRT dataPoint = getDataPoint(dataPointId);
        return dataPoint != null && dataPoint.getLifecycleState() == ILifecycleState.RUNNING;
    }

    @Override
    public void removeDataPoint(DataPointRT dataPoint) {
        dataPoint.ensureState(ILifecycleState.TERMINATED);
        dataPointCache.remove(dataPoint.getId(), dataPoint);
    }

    @Override
    public void removeDataSource(DataSourceRT<? extends DataSourceVO> dataSource) {
        dataSource.ensureState(ILifecycleState.TERMINATED);
        runningDataSources.remove(dataSource.getId(), dataSource);
    }

    @Override
    @Nullable
    public DataPointRT getDataPoint(int dataPointId) {
        return dataPointCache.get(dataPointId);
    }

    @Override
    public Collection<DataPointRT> getRunningDataPoints() {
        return dataPointCache.values();
    }

    @Override
    public void addDataPointListener(int dataPointId, DataPointListener l) {
        dataPointListeners.compute(dataPointId, (k, v) -> DataPointEventMulticaster.add(v, l));
    }

    @Override
    public void removeDataPointListener(int dataPointId, DataPointListener l) {
        dataPointListeners.compute(dataPointId, (k, v) -> DataPointEventMulticaster.remove(v, l));
    }

    @Override
    public DataPointListener getDataPointListeners(int dataPointId) {
        return dataPointListeners.get(dataPointId);
    }

    //
    // Point values
    @Override
    public void setDataPointValue(int dataPointId, DataValue value, SetPointSource source) {
        setDataPointValue(dataPointId, new PointValueTime(value, Common.timer.currentTimeMillis()), source);
    }

    @Override
    public void setDataPointValue(int dataPointId, PointValueTime valueTime, SetPointSource source) {
        DataPointRT dataPoint = dataPointCache.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        if (!dataPoint.getPointLocator().isSettable())
            throw new RTException("Point is not settable");

        // Tell the data source to set the value of the point.
        DataSourceRT<? extends DataSourceVO> ds = dataPoint.getDataSource();
        ds.setPointValue(dataPoint, valueTime, source);
    }

    @Override
    public void relinquish(int dataPointId) {
        DataPointRT dataPoint = dataPointCache.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        if (!dataPoint.getPointLocator().isSettable())
            throw new RTException("Point is not settable");
        if (!dataPoint.getPointLocator().isRelinquishable())
            throw new RTException("Point cannot be relinquished");

        // Tell the data source to relinquish value of the point.
        DataSourceRT<? extends DataSourceVO> ds = dataPoint.getDataSource();
        ds.relinquish(dataPoint);
    }

    @Override
    public void forcePointRead(int dataPointId) {
        DataPointRT dataPoint = dataPointCache.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        // Tell the data source to read the point value;
        DataSourceRT<? extends DataSourceVO> ds = dataPoint.getDataSource();
        ds.forcePointRead(dataPoint);
    }

    @Override
    public void forceDataSourcePoll(int dataSourceId) {
        DataSourceRT<? extends DataSourceVO> dataSource = getRunningDataSource(dataSourceId);
        dataSource.forcePoll();
    }

    @Override
    public long purgeDataPointValues() {
        long count = Common.databaseProxy.newPointValueDao().deleteAllPointData();
        dataPointDao.getAll(point -> {
            Common.databaseProxy.getPointValueCacheDao().removeAllValues(point);
            DataPointRT rt = getDataPoint(point.getId());
            if(rt != null) {
                rt.invalidateCache(false);
            }
        });
        return count;
    }

    @Override
    public void purgeDataPointValuesWithoutCount() {
        Common.databaseProxy.newPointValueDao().deleteAllPointDataWithoutCount();
        dataPointDao.getAll(point -> {
            Common.databaseProxy.getPointValueCacheDao().removeAllValues(point);
            DataPointRT rt = getDataPoint(point.getId());
            if(rt != null) {
                rt.invalidateCache(false);
            }
        });
    }

    @Override
    public long purgeDataPointValues(DataPointVO vo, int periodType, int periodCount) {
        long before = DateUtils.minus(Common.timer.currentTimeMillis(), periodType, periodCount);
        return purgeDataPointValues(vo, before);
    }

    @Override
    public long purgeDataPointValues(DataPointVO vo) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValues(vo);
        Common.databaseProxy.getPointValueCacheDao().removeAllValues(vo);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.invalidateCache(false);
        }
        return count;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(DataPointVO vo) {
        boolean deleted = Common.databaseProxy.newPointValueDao().deletePointValuesWithoutCount(vo);
        Common.databaseProxy.getPointValueCacheDao().removeAllValues(vo);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.invalidateCache(false);
        }
        return deleted;
    }

    @Override
    public long purgeDataPointValue(DataPointVO vo, long ts, PointValueDao dao){
        long count = dao.deletePointValue(vo, ts);
        Common.databaseProxy.getPointValueCacheDao().removeValueAt(vo, ts);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.invalidateCache(false);
        }
        return count;
    }

    @Override
    public long purgeDataPointValues(DataPointVO vo, long before) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValuesBefore(vo, before);
        Common.databaseProxy.getPointValueCacheDao().removeValuesBefore(vo, before);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.invalidateCache(false);
        }
        return count;
    }

    @Override
    public long purgeDataPointValuesBetween(DataPointVO vo, long startTime, long endTime) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValuesBetween(vo, startTime, endTime);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.invalidateCache(false);
        }else {
            Common.databaseProxy.getPointValueCacheDao().removeValuesBetween(vo, startTime, endTime);
        }
        return count;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(DataPointVO vo, long before) {
        boolean deleted = Common.databaseProxy.newPointValueDao().deletePointValuesBeforeWithoutCount(vo, before);
        Common.databaseProxy.getPointValueCacheDao().removeValuesBefore(vo, before);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.invalidateCache(false);
        }
        return deleted;
    }

    //
    //
    // Publishers
    //
    @Override
    public Collection<PublisherRT<? extends PublishedPointVO>> getRunningPublishers() {
        return runningPublishers.values();
    }

    @Override
    @Nullable
    public PublisherRT<? extends PublishedPointVO> getRunningPublisher(int publisherId) {
        return runningPublishers.get(publisherId);
    }

    @Override
    public boolean isPublisherRunning(int publisherId) {
        DataSourceRT<? extends DataSourceVO> publisher = runningDataSources.get(publisherId);
        return publisher != null && publisher.getLifecycleState() == ILifecycleState.RUNNING;
    }

    @Override
    public void startPublisher(PublisherVO<? extends PublishedPointVO> vo) {
        // Ensure that the publisher is saved and enabled.
        Assert.isTrue(vo.getId() > 0, "Publisher must be saved");
        Assert.isTrue(vo.isEnabled(), "Publisher must be enabled");
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);

        PublisherRT<? extends PublishedPointVO> publisher = runningPublishers.computeIfAbsent(vo.getId(), k -> vo.createPublisherRT());

        long startTime = System.nanoTime();

        // Create and initialize the runtime version of the data source.
        publisher.initialize(false);

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        stateMessage = new TranslatableMessage("runtimeManager.initialize.publisher", vo.getName(), duration);
        LOG.info(String.format("%s took %dms to start", publisher.readableIdentifier(), duration));
    }

    @Override
    public void stopPublisher(int publisherId) {
        PublisherRT<? extends PublishedPointVO> publisher = runningPublishers.get(publisherId);
        if (publisher != null) {
            long startTime = System.nanoTime();

            try {
                publisher.terminate();
            } catch (Exception e) {
                LOG.error("Error while terminating " + publisher.readableIdentifier(), e);
            }

            try {
                publisher.joinTermination();
            } catch (Exception e) {
                LOG.error("Error while waiting for " + publisher.readableIdentifier() + " to terminate", e);
            }

            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            LOG.info(String.format("%s stopped in %dms", publisher.readableIdentifier(), duration));
        }
    }

    public void removePublisher(PublisherRT<? extends PublishedPointVO> publisher) {
        publisher.ensureState(ILifecycleState.TERMINATED);
        runningPublishers.remove(publisher.getId(), publisher);
    }
}
