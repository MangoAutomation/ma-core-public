/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.util.Assert;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.RuntimeManagerDefinition;
import com.serotonin.m2m2.rt.dataImage.DataPointEventMulticaster;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.dataSource.PollingDataSource;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.util.ExceptionListWrapper;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.ILifecycleState;

public class RuntimeManagerImpl implements RuntimeManager {
    private static final Log LOG = LogFactory.getLog(RuntimeManagerImpl.class);

    private final ConcurrentMap<Integer, DataSourceRT<? extends DataSourceVO>> runningDataSources = new ConcurrentHashMap<>();

    /**
     * Provides a quick lookup map of the running data points.
     */
    private final ConcurrentMap<Integer, DataPointRT> dataPoints = new ConcurrentHashMap<>();

    /**
     * The list of point listeners, kept here such that listeners can be notified of point initializations (i.e. a
     * listener can register itself before the point is enabled).
     */
    private final ConcurrentMap<Integer, DataPointListener> dataPointListeners = new ConcurrentHashMap<>();

    /**
     * Store of enabled publishers
     */
    private final List<PublisherRT<?>> runningPublishers = new CopyOnWriteArrayList<>();

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
        Map<DataSourceDefinition.StartPriority, List<DataSourceVO>> priorityMap = new HashMap<>();
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
        for (DataSourceDefinition.StartPriority startPriority : DataSourceDefinition.StartPriority.values()) {
            List<DataSourceVO> priorityList = priorityMap.get(startPriority);
            if (priorityList != null) {
                DataSourceGroupInitializer initializer = new DataSourceGroupInitializer(
                        executorService, startupThreads, startPriority);
                pollingRound.addAll(initializer.process(priorityList));
            }
        }

        // Tell the data sources to start polling. Delaying the polling start gives the data points a chance to
        // initialize such that point listeners in meta points and set point handlers can run properly.
        for (DataSourceVO config : pollingRound)
            startDataSourcePolling(config);

        // Run everything else.
        startRTMDefs(defs, safe, rtmdIndex, Integer.MAX_VALUE);

        // Start the publishers that are enabled
        long pubStart = Common.timer.currentTimeMillis();
        List<PublisherVO<? extends PublishedPointVO>> publishers = publisherDao.getAll();
        LOG.info("Starting " + publishers.size() + " Publishers...");
        for (PublisherVO<? extends PublishedPointVO> vo : publishers) {
            LOG.info("Starting publisher: " + vo.getName());
            if (vo.isEnabled()) {
                if (safe) {
                    vo.setEnabled(false);
                    publisherDao.update(vo.getId(), vo);
                }
                else
                    startPublisher(vo);
            }
        }
        LOG.info(publishers.size() + " Publisher's started in " +  (Common.timer.currentTimeMillis() - pubStart) + "ms");

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

        for (PublisherRT<? extends PublishedPointVO> publisher : runningPublishers)
            stopPublisher(publisher.getId());

        // Get the RTM defs and sort by reverse init priority.
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        defs.sort((def1, def2) -> def2.getInitializationPriority() - def1.getInitializationPriority());

        // Stop everything with priority up to and including 5.
        int rtmdIndex = stopRTMDefs(defs, 0, 5);

        // Stop data sources in reverse start priority order.
        Map<DataSourceDefinition.StartPriority, List<DataSourceRT<? extends DataSourceVO>>> priorityMap = new HashMap<>();
        for (Entry<Integer, DataSourceRT<? extends DataSourceVO>> entry : runningDataSources.entrySet()) {
            DataSourceRT<? extends DataSourceVO> rt = entry.getValue();
            List<DataSourceRT<? extends DataSourceVO>> priorityList = priorityMap.computeIfAbsent(rt.getVo().getDefinition().getStartPriority(), k -> new ArrayList<>());
            priorityList.add(rt);
        }

        int dataSourceShutdownThreads = Common.envProps.getInt("runtime.datasource.shutdownThreads", 1);
        DataSourceDefinition.StartPriority[] priorities = DataSourceDefinition.StartPriority.values();
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
    public @NonNull DataSourceRT<? extends DataSourceVO> getRunningDataSource(int dataSourceId) {
        DataSourceRT<? extends DataSourceVO> ds = runningDataSources.get(dataSourceId);
        if (ds == null) {
            throw new RTException(String.format("Data source is not running: id=%d, type=%s",
                    dataSourceId, getClass()));
        }
        return ds;
    }

    @Override
    public List<? extends DataSourceRT<?>> getRunningDataSources() {
        return new ArrayList<>(runningDataSources.values());
    }

    @Override
    public boolean isDataSourceRunning(int dataSourceId) {
        return runningDataSources.containsKey(dataSourceId);
    }

    @Override
    public void startDataSource(DataSourceVO vo) {
        Assert.isTrue(vo.getId() > 0, "Data source must be saved");
        Assert.isTrue(vo.isEnabled(), "Data source must be enabled");

        if (initializeDataSourceStartup(vo)) {
            startDataSourcePolling(vo);
        }
    }

    /**
     * Only to be used at startup as the synchronization has been reduced for performance
     * @param vo data source VO
     * @return true if the data source was initialized, false if it was already running
     */
    @Override
    public boolean initializeDataSourceStartup(DataSourceVO vo) {
        // Ensure that the data source is enabled.
        Assert.isTrue(vo.getId() > 0, "Data source must be saved");
        Assert.isTrue(vo.isEnabled(), "Data source must be enabled");

        AtomicBoolean wasCreated = new AtomicBoolean();
        DataSourceRT<? extends DataSourceVO> dataSource = runningDataSources.computeIfAbsent(vo.getId(), k -> {
            wasCreated.set(true);
            return vo.createDataSourceRT();
        });

        // If the data source is already running, just quit.
        if (!wasCreated.get())
            return false;

        long startTime = System.nanoTime();

        // Create and initialize the runtime version of the data source.
        dataSource.initialize(false);

        // Add the enabled points to the data source.
        List<DataPointWithEventDetectors> dataSourcePoints = dataPointDao.getDataPointsForDataSourceStart(vo.getId());

        //Startup multi threaded
        int pointsPerThread = Common.envProps.getInt("runtime.datapoint.startupThreads.pointsPerThread", 1000);
        int startupThreads = Common.envProps.getInt("runtime.datapoint.startupThreads", Runtime.getRuntime().availableProcessors());
        PointValueCacheDao pointValueCacheDao = Common.databaseProxy.getPointValueCacheDao();
        DataPointGroupInitializer pointInitializer = new DataPointGroupInitializer(executorService, startupThreads, pointValueCacheDao);
        pointInitializer.initialize(dataSourcePoints, pointsPerThread);

        //Signal to the data source that all points are added.
        dataSource.initialized();

        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        int took = (int)((double)duration/(double)1000000);
        stateMessage = new TranslatableMessage("runtimeManager.initialize.dataSource", vo.getName(), took);
        LOG.info(new TranslatableMessage("runtimeManager.initialize.dataSource", vo.getName(), took).translate(Common.getTranslations()));

        return true;
    }

    private void startDataSourcePolling(DataSourceVO vo) {
        DataSourceRT<? extends DataSourceVO> dataSource = getRunningDataSource(vo.getId());
        dataSource.beginPolling();
    }

    @Override
    public void stopDataSource(int dataSourceId) {
        stopDataSourceShutdown(dataSourceId);
    }

    /**
     * Should only be called at Shutdown as synchronization has been reduced for performance
     */
    @Override
    public void stopDataSourceShutdown(int id) {
        DataSourceRT<? extends DataSourceVO> dataSource = runningDataSources.remove(id);
        if (dataSource == null) return;

        long now = Common.timer.currentTimeMillis();
        try {
            //Signal we are going down
            dataSource.terminating();
        } catch (Exception e) {
            LOG.error("Failed to signal termination to data source: " + dataSource.readableIdentifier(), e);
        }

        try {
            List<Integer> pointIds = new ArrayList<>();
            // Stop the data points.
            for (DataPointRT p : dataPoints.values()) {
                if (p.getDataSourceId() == id) {
                    stopDataPointShutdown(dataSource, p);
                    pointIds.add(p.getId());
                }
            }

            //Terminate all events at once
            Common.eventManager.cancelEventsForDataPoints(pointIds);
        } catch (Exception e) {
            LOG.error("Failed to stop points for data source: " + dataSource.readableIdentifier(), e);
        }

        try {
            dataSource.terminate();
        } catch (Exception e) {
            LOG.error("Error while terminating data source: " + dataSource.readableIdentifier(), e);
        }

        try {
            dataSource.joinTermination();
        } catch (Exception e) {
            LOG.error("Error waiting for data source to terminate: " + dataSource.readableIdentifier(), e);
        }

        LOG.info(String.format("Data source [%s] stopped in %dms",
                dataSource.readableIdentifier(), Common.timer.currentTimeMillis() - now));
    }

    //
    //
    // Data points
    //
    @Override
    public void startDataPoint(DataPointWithEventDetectors vo) {
        Assert.isTrue(vo.getDataPoint().isEnabled(), "Attempting to start disabled data point.");
        DataPointWithEventDetectorsAndCache dp = new DataPointWithEventDetectorsAndCache(vo, null);
        startDataPoint(dp);
    }

    @Override
    public void stopDataPoint(int id) {
        synchronized (dataPoints) {
            // Remove this point from the data image if it is there. If not, just quit.
            DataPointRT p = dataPoints.remove(id);

            // Remove it from the data source, and terminate it.
            if (p != null) {
                try{
                    getRunningDataSource(p.getDataSourceId()).removeDataPoint(p);
                }catch(Exception e){
                    LOG.error("Failed to stop point RT with ID: " + id
                            + " stopping point."
                            , e);
                }

                DataPointListener l = getDataPointListeners(id);
                if (l != null)
                    try {
                        l.pointTerminated(p.getVO());
                    } catch(ExceptionListWrapper e) {
                        LOG.warn("Exceptions in point terminated method.");
                        for(Exception e2 : e.getExceptions())
                            LOG.warn("Listener exception: " + e2.getMessage(), e2);
                    }
                p.terminate();
                Common.eventManager.cancelEventsForDataPoint(p.getId());
            }
        }
    }

    private void startDataPoint(DataPointWithEventDetectorsAndCache vo) {
        synchronized (dataPoints) {
            startDataPointStartup(vo);
        }
    }

    /**
     * Only to be used at startup as synchronization has been reduced for performance
     * @param vo data point
     */
    @Override
    public void startDataPointStartup(DataPointWithEventDetectorsAndCache vo) {
        Assert.isTrue(vo.getDataPoint().isEnabled(), "Data point not enabled");

        // Only add the data point if its data source is enabled.
        DataSourceRT<? extends DataSourceVO> ds = getRunningDataSource(vo.getDataPoint().getDataSourceId());

        // Change the VO into a data point implementation.
        DataPointRT dataPoint = new DataPointRT(vo, vo.getDataPoint().getPointLocator().createRuntime(), ds.getVo(),
                vo.getInitialCache(), Common.databaseProxy.newPointValueDao(), Common.databaseProxy.getPointValueCacheDao());

        // Add/update it in the data image.
        synchronized (dataPoints) {
            dataPoints.compute(dataPoint.getId(), (k, rt) -> {
                if(rt != null) {
                    try{
                        getRunningDataSource(rt.getDataSourceId()).removeDataPoint(rt);
                    }catch(Exception e){
                        LOG.error("Failed to stop point RT with ID: " + vo.getDataPoint().getId()
                                + " stopping point."
                                , e);
                    }
                    DataPointListener l = getDataPointListeners(vo.getDataPoint().getId());
                    if (l != null)
                        try {
                            l.pointTerminated(vo.getDataPoint());
                        } catch(ExceptionListWrapper e) {
                            LOG.warn("Exceptions in point terminated listeners' methods.");
                            for(Exception e2 : e.getExceptions())
                                LOG.warn("Listener exception: " + e2.getMessage(), e2);
                        } catch(Exception e) {
                            LOG.warn("Exception in point terminated listener's method: " + e.getMessage(), e);
                        }
                    rt.terminate();
                    Common.eventManager.cancelEventsForDataPoint(rt.getId());
                }
                return dataPoint;
            });
        }

        // Initialize it.
        dataPoint.initialize(false);

        //If we are a polling data source then we need to wait to start our interval logging
        // until the first poll due to quantization
        boolean isPolling = ds instanceof PollingDataSource;

        //If we are not polling go ahead and start the interval logging, otherwise we will let the data source do it on the first poll
        if(!isPolling)
            dataPoint.initializeIntervalLogging(0L, false);

        DataPointListener l = getDataPointListeners(vo.getDataPoint().getId());
        if (l != null)
            try {
                l.pointInitialized();
            } catch(ExceptionListWrapper e) {
                LOG.warn("Exceptions in point initialized listeners' methods.");
                for(Exception e2 : e.getExceptions())
                    LOG.warn("Listener exception: " + e2.getMessage(), e2);
            } catch(Exception e) {
                LOG.warn("Exception in point initialized listener's method: " + e.getMessage(), e);
            }

        // Add/update it in the data source.
        try{
            ds.addDataPoint(dataPoint);
        }catch(Exception e){
            //This can happen if there is a corrupt DB with a point for a different
            // data source type linked to this data source...
            LOG.error("Failed to start point with xid: " + dataPoint.getVO().getXid()
                    + " disabling point."
                    , e);
            //TODO Fire Alarm to warn user.
            dataPoint.getVO().setEnabled(false);
            dataPointDao.saveEnabledColumn(dataPoint.getVO());
            stopDataPoint(dataPoint.getId()); //Stop it
        }
    }

    /**
     * Only to be used at shutdown as synchronization has been reduced for performance
     */
    private void stopDataPointShutdown(DataSourceRT<? extends DataSourceVO> dataSource, DataPointRT dp) {
        synchronized (dataPoints) {
            // Remove this point from the data image if it is there. If not, just quit.
            if (!dataPoints.remove(dp.getId(), dp)) {
                return;
            }
        }

        try{
            dataSource.removeDataPoint(dp);
        }catch(Exception e){
            LOG.error("Failed to stop point RT with ID: " + dp.getId()
                            + " stopping point."
                    , e);
        }
        DataPointListener l = getDataPointListeners(dp.getId());
        if (l != null)
            try {
                l.pointTerminated(dp.getVO());
            } catch(ExceptionListWrapper e) {
                LOG.warn("Exceptions in point terminated method.");
                for(Exception e2 : e.getExceptions())
                    LOG.warn("Listener exception: " + e2.getMessage(), e2);
            }
        //Stop data point but don't cancel events until after to do this in bulk.
        dp.terminate();
    }

    @Override
    public boolean isDataPointRunning(int dataPointId) {
        return dataPoints.get(dataPointId) != null;
    }

    @Override
    public DataPointRT getDataPoint(int dataPointId) {
        return dataPoints.get(dataPointId);
    }

    @Override
    public List<DataPointRT> getRunningDataPoints() {
        return new ArrayList<>(dataPoints.values());
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
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        if (!dataPoint.getPointLocator().isSettable())
            throw new RTException("Point is not settable");

        // Tell the data source to set the value of the point.
        DataSourceRT<? extends DataSourceVO> ds = getRunningDataSource(dataPoint.getDataSourceId());
        ds.setPointValue(dataPoint, valueTime, source);
    }

    @Override
    public void relinquish(int dataPointId) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        if (!dataPoint.getPointLocator().isSettable())
            throw new RTException("Point is not settable");
        if (!dataPoint.getPointLocator().isRelinquishable())
            throw new RTException("Point cannot be relinquished");

        // Tell the data source to relinquish value of the point.
        DataSourceRT<? extends DataSourceVO> ds = getRunningDataSource(dataPoint.getDataSourceId());
        ds.relinquish(dataPoint);
    }

    @Override
    public void forcePointRead(int dataPointId) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        // Tell the data source to read the point value;
        DataSourceRT<? extends DataSourceVO> ds = getRunningDataSource(dataPoint.getDataSourceId());
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
            DataPointRT rt = getDataPoint(point.getId());
            if(rt != null) {
                rt.resetValues(Long.MAX_VALUE);
            }else {
                Common.databaseProxy.getPointValueCacheDao().deleteCache(point);
            }
        });
        return count;
    }

    @Override
    public void purgeDataPointValuesWithoutCount() {
        Common.databaseProxy.newPointValueDao().deleteAllPointDataWithoutCount();
        dataPointDao.getAll(point -> {
            DataPointRT rt = getDataPoint(point.getId());
            if(rt != null) {
                rt.resetValues(Long.MAX_VALUE);
            }else {
                Common.databaseProxy.getPointValueCacheDao().deleteCache(point);
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
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.resetValues(Long.MAX_VALUE);
        }else {
            Common.databaseProxy.getPointValueCacheDao().deleteCache(vo);
        }
        return count;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(DataPointVO vo) {
        boolean deleted = Common.databaseProxy.newPointValueDao().deletePointValuesWithoutCount(vo);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.resetValues(Long.MAX_VALUE);
        }else {
            Common.databaseProxy.getPointValueCacheDao().deleteCache(vo);
        }
        return deleted;
    }

    @Override
    public long purgeDataPointValue(DataPointVO vo, long ts, PointValueDao dao){
        long count = dao.deletePointValue(vo, ts);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.resetValues();
        }else {
            Common.databaseProxy.getPointValueCacheDao().deleteCachedValue(vo, ts);
        }
        return count;

    }

    @Override
    public long purgeDataPointValues(DataPointVO vo, long before) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValuesBefore(vo, before);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.resetValues(before);
        }else {
            Common.databaseProxy.getPointValueCacheDao().deleteCachedValuesBefore(vo, before);
        }
        return count;
    }

    @Override
    public long purgeDataPointValuesBetween(DataPointVO vo, long startTime, long endTime) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValuesBetween(vo, startTime, endTime);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.resetValues(endTime);
        }else {
            Common.databaseProxy.getPointValueCacheDao().deleteCachedValuesBetween(vo, startTime, endTime);
        }
        return count;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(DataPointVO vo, long before) {
        boolean deleted = Common.databaseProxy.newPointValueDao().deletePointValuesBeforeWithoutCount(vo, before);
        DataPointRT rt = getDataPoint(vo.getId());
        if(rt != null) {
            rt.resetValues(before);
        }else {
            Common.databaseProxy.getPointValueCacheDao().deleteCachedValuesBefore(vo, before);
        }
        return deleted;
    }

    //
    //
    // Publishers
    //
    @Override
    public List<PublisherRT<?>> getRunningPublishers() {
        return new ArrayList<>(runningPublishers);
    }

    @Override
    public PublisherRT<?> getRunningPublisher(int publisherId) {
        for (PublisherRT<?> publisher : runningPublishers) {
            if (publisher.getId() == publisherId)
                return publisher;
        }
        return null;
    }

    @Override
    public boolean isPublisherRunning(int publisherId) {
        return getRunningPublisher(publisherId) != null;
    }

    @Override
    public PublisherVO<? extends PublishedPointVO> getPublisher(int publisherId) {
        return publisherDao.get(publisherId);
    }

    @Override
    public void startPublisher(PublisherVO<? extends PublishedPointVO> vo) {
        long startTime = System.nanoTime();
        synchronized (runningPublishers) {
            // If the publisher is already running, just quit.
            if (isPublisherRunning(vo.getId()))
                return;

            // Ensure that the publisher is enabled.
            Assert.isTrue(vo.isEnabled(), "Publisher not enabled");

            // Create and start the runtime version of the publisher.
            PublisherRT<?> publisher = vo.createPublisherRT();
            publisher.initialize();

            // Add it to the list of running publishers.
            runningPublishers.add(publisher);
        }

        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        LOG.info("Publisher '" + vo.getName() + "' took " + (double)duration/(double)1000000 + "ms to start");
        stateMessage = new TranslatableMessage("runtimeManager.initialize.publisher", vo.getName(), (double)duration/(double)1000000);
    }

    @Override
    public void stopPublisher(int publisherId) {
        synchronized (runningPublishers) {
            PublisherRT<?> publisher = getRunningPublisher(publisherId);
            if (publisher == null)
                return;

            publisher.terminate();
            publisher.joinTermination();
            runningPublishers.remove(publisher);
        }
    }
}
