/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
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

public class RuntimeManagerImpl implements RuntimeManager {
    private static final Log LOG = LogFactory.getLog(RuntimeManagerImpl.class);

    private final ConcurrentMap<Integer, DataSourceRT<? extends DataSourceVO>> runningDataSources = new ConcurrentHashMap<>();

    /**
     * Provides a quick lookup map of the running data points.
     */
    private final ConcurrentMap<Integer, DataPointRT> dataPoints = new ConcurrentHashMap<Integer, DataPointRT>();

    /**
     * The list of point listeners, kept here such that listeners can be notified of point initializations (i.e. a
     * listener can register itself before the point is enabled).
     */
    private final ConcurrentMap<Integer, DataPointListener> dataPointListeners = new ConcurrentHashMap<Integer, DataPointListener>();

    /**
     * Store of enabled publishers
     */
    private final List<PublisherRT<?>> runningPublishers = new CopyOnWriteArrayList<PublisherRT<?>>();

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
    private int state = PRE_INITIALIZE;

    @Override
    public int getState(){
        return state;
    }

    @Override
    public TranslatableMessage getStateMessage() {
        switch(state) {
            case PRE_INITIALIZE:
            case INITIALIZE:
                return stateMessage;
            case RUNNING:
                return new TranslatableMessage("startup.state.running");
            case TERMINATE:
            case POST_TERMINATE:
            case TERMINATED:
            default:
                return new TranslatableMessage("shutdown.state.preTerminate");
        }
    }

    //
    // Lifecycle

    @Override
    synchronized public void initialize(boolean safe) {
        if (state != PRE_INITIALIZE)
            return;

        // Set the started indicator to true.
        state = INITIALIZE;

        //Get the RTM defs from modules
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        Collections.sort(defs, new Comparator<RuntimeManagerDefinition>() {
            @Override
            public int compare(RuntimeManagerDefinition def1, RuntimeManagerDefinition def2) {
                return def1.getInitializationPriority() - def2.getInitializationPriority();
            }
        });

        // Start everything with priority up to and including 4.
        int rtmdIndex = startRTMDefs(defs, safe, 0, 4);

        // Initialize data sources that are enabled. Start by organizing all enabled data sources by start priority.
        List<DataSourceVO> configs = null;
        configs = DataSourceDao.getInstance().getAll();
        Map<DataSourceDefinition.StartPriority, List<DataSourceVO>> priorityMap = new HashMap<DataSourceDefinition.StartPriority, List<DataSourceVO>>();
        for (DataSourceVO config : configs) {
            if (config.isEnabled()) {
                if (safe) {
                    config.setEnabled(false);
                    DataSourceDao.getInstance().update(config.getId(), config);
                }
                else if (config.getDefinition() != null) {
                    List<DataSourceVO> priorityList = priorityMap.get(config.getDefinition().getStartPriority());
                    if (priorityList == null) {
                        priorityList = new ArrayList<DataSourceVO>();
                        priorityMap.put(config.getDefinition().getStartPriority(), priorityList);
                    }
                    priorityList.add(config);
                }
            }
        }

        // Initialize the prioritized data sources. Start the polling later.
        List<DataSourceVO> pollingRound = new ArrayList<>();
        int startupThreads = Common.envProps.getInt("runtime.datasource.startupThreads", 1);
        boolean useMetrics = Common.envProps.getBoolean("runtime.datasource.logStartupMetrics", false);
        ExecutorService executor = Common.getBean(ExecutorService.class);
        for (DataSourceDefinition.StartPriority startPriority : DataSourceDefinition.StartPriority.values()) {
            List<DataSourceVO> priorityList = priorityMap.get(startPriority);
            if (priorityList != null) {
                DataSourceGroupInitializer initializer = new DataSourceGroupInitializer(
                        useMetrics, executor, startupThreads, startPriority);
                pollingRound.addAll(initializer.process(priorityList));
            }
        }

        // Tell the data sources to start polling. Delaying the polling start gives the data points a chance to
        // initialize such that point listeners in meta points and set point handlers can run properly.
        for (DataSourceVO config : pollingRound)
            startDataSourcePolling(config);

        // Run everything else.
        rtmdIndex = startRTMDefs(defs, safe, rtmdIndex, Integer.MAX_VALUE);

        // Start the publishers that are enabled
        long pubStart = Common.timer.currentTimeMillis();
        List<PublisherVO<? extends PublishedPointVO>> publishers = PublisherDao.getInstance().getAll();
        LOG.info("Starting " + publishers.size() + " Publishers...");
        for (PublisherVO<? extends PublishedPointVO> vo : publishers) {
            LOG.info("Starting publisher: " + vo.getName());
            if (vo.isEnabled()) {
                if (safe) {
                    vo.setEnabled(false);
                    PublisherDao.getInstance().update(vo.getId(), vo);
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
        this.state = RUNNING;
    }

    @Override
    synchronized public void terminate() {
        if (state != RUNNING)
            return;
        state = TERMINATE;

        for (PublisherRT<? extends PublishedPointVO> publisher : runningPublishers)
            stopPublisher(publisher.getId());

        // Get the RTM defs and sort by reverse init priority.
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        Collections.sort(defs, new Comparator<RuntimeManagerDefinition>() {
            @Override
            public int compare(RuntimeManagerDefinition def1, RuntimeManagerDefinition def2) {
                return def2.getInitializationPriority() - def1.getInitializationPriority();
            }
        });

        // Stop everything with priority up to and including 5.
        int rtmdIndex = stopRTMDefs(defs, 0, 5);

        // Stop data sources in reverse start priority order.
        Map<DataSourceDefinition.StartPriority, List<DataSourceRT<? extends DataSourceVO>>> priorityMap = new HashMap<>();
        for (Entry<Integer, DataSourceRT<? extends DataSourceVO>> entry : runningDataSources.entrySet()) {
            DataSourceRT<? extends DataSourceVO> rt = entry.getValue();
            List<DataSourceRT<? extends DataSourceVO>> priorityList = priorityMap.get(rt.getVo().getDefinition().getStartPriority());
            if (priorityList == null) {
                priorityList = new ArrayList<>();
                priorityMap.put(rt.getVo().getDefinition().getStartPriority(), priorityList);
            }
            priorityList.add(rt);
        }

        int dataSourceShutdownThreads = Common.envProps.getInt("runtime.datasource.shutdownThreads", 1);
        boolean useMetrics = Common.envProps.getBoolean("runtime.datasource.logStartupMetrics", false);
        ExecutorService executor = Common.getBean(ExecutorService.class);
        DataSourceDefinition.StartPriority[] priorities = DataSourceDefinition.StartPriority.values();
        for (int i = priorities.length - 1; i >= 0; i--) {
            List<DataSourceRT<? extends DataSourceVO>> priorityList = priorityMap.get(priorities[i]);
            if (priorityList != null) {
                DataSourceGroupTerminator initializer = new DataSourceGroupTerminator(
                        useMetrics, executor, dataSourceShutdownThreads, priorities[i]);
                initializer.process(priorityList);
            }
        }

        // Run everything else.
        rtmdIndex = stopRTMDefs(defs, rtmdIndex, Integer.MIN_VALUE);
    }

    @Override
    public void joinTermination() {
        if(state != TERMINATE)
            return;
        state = POST_TERMINATE;

        for (Entry<Integer, DataSourceRT<? extends DataSourceVO>> entry : runningDataSources.entrySet()) {
            DataSourceRT<? extends DataSourceVO> dataSource = entry.getValue();
            try {
                dataSource.joinTermination();
            }
            catch (ShouldNeverHappenException e) {
                LOG.error("Error stopping data source " + dataSource.getId(), e);
            }
        }
        state = TERMINATED;
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
    public DataSourceRT<? extends DataSourceVO> getRunningDataSource(int dataSourceId) {
        return runningDataSources.get(dataSourceId);
    }

    @Override
    public List<? extends DataSourceRT<?>> getRunningDataSources() {
        return new ArrayList<>(runningDataSources.values());
    }

    @Override
    public boolean isDataSourceRunning(int dataSourceId) {
        return getRunningDataSource(dataSourceId) != null;
    }

    @Override
    public DataSourceVO getDataSource(int dataSourceId) {
        return DataSourceDao.getInstance().get(dataSourceId);
    }

    @Override
    public void deleteDataSource(int dataSourceId) {
        stopDataSource(dataSourceId);
        DataSourceDao.getInstance().delete(dataSourceId);
        Common.eventManager.cancelEventsForDataSource(dataSourceId);
    }

    @Override
    public void insertDataSource(DataSourceVO vo) {

        // In this case it is new data source, we need to save to the database first so that it has a proper id.
        DataSourceDao.getInstance().insert(vo);

        // If the data source is enabled, start it.
        if (vo.isEnabled()) {
            if (initializeDataSource(vo))
                startDataSourcePolling(vo);
        }
    }

    @Override
    public void updateDataSource(DataSourceVO existing, DataSourceVO vo) {
        // If the data source is running, stop it.
        stopDataSource(vo.getId());

        DataSourceDao.getInstance().update(existing, vo);

        // If the data source is enabled, start it.
        if (vo.isEnabled()) {
            if (initializeDataSource(vo))
                startDataSourcePolling(vo);
        }
    }

    private boolean initializeDataSource(DataSourceVO vo) {
        synchronized (runningDataSources) {
            return initializeDataSourceStartup(vo);
        }
    }

    /**
     * Only to be used at startup as the synchronization has been reduced for performance
     * @param vo
     * @return
     */
    @Override
    public boolean initializeDataSourceStartup(DataSourceVO vo) {
        long startTime = System.nanoTime();

        // If the data source is already running, just quit.
        if (isDataSourceRunning(vo.getId()))
            return false;

        // Ensure that the data source is enabled.
        Assert.isTrue(vo.isEnabled(), "Data source not enabled.");

        // Create and initialize the runtime version of the data source.
        DataSourceRT<? extends DataSourceVO> dataSource = vo.createDataSourceRT();
        dataSource.initialize();

        // Add it to the list of running data sources.
        synchronized(runningDataSources) {
            runningDataSources.put(dataSource.getId(), dataSource);
        }

        // Add the enabled points to the data source.
        List<DataPointWithEventDetectors> dataSourcePoints = DataPointDao.getInstance().getDataPointsForDataSourceStart(vo.getId());

        //Startup multi threaded
        int pointsPerThread = Common.envProps.getInt("runtime.datapoint.startupThreads.pointsPerThread", 1000);
        int startupThreads = Common.envProps.getInt("runtime.datapoint.startupThreads", Runtime.getRuntime().availableProcessors());
        boolean useMetrics = Common.envProps.getBoolean("runtime.datapoint.logStartupMetrics", false);
        ExecutorService executor = Common.getBean(ExecutorService.class);
        DataPointGroupInitializer pointInitializer = new DataPointGroupInitializer(useMetrics, executor, startupThreads, Common.databaseProxy.newPointValueDao());
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
        if (dataSource != null)
            dataSource.beginPolling();
    }

    private void stopDataSource(int id) {
        synchronized (runningDataSources) {
            stopDataSourceShutdown(id);
        }
    }

    /**
     * Should only be called at Shutdown as synchronization has been reduced for performance
     */
    @Override
    public void stopDataSourceShutdown(int id) {

        DataSourceRT<? extends DataSourceVO> dataSource = getRunningDataSource(id);
        if (dataSource == null)
            return;
        try{
            long now = Common.timer.currentTimeMillis();
            //Signal we are going down
            dataSource.terminating();

            List<Integer> pointIds = new ArrayList<>();
            // Stop the data points.
            for (DataPointRT p : dataPoints.values()) {
                if (p.getDataSourceId() == id) {
                    stopDataPointShutdown(p.getVO());
                    pointIds.add(p.getId());
                }
            }

            //Terminate all events at once
            Common.eventManager.cancelEventsForDataPoints(pointIds);

            synchronized (runningDataSources) {
                runningDataSources.remove(dataSource.getId());
            }
            dataSource.terminate();
            dataSource.joinTermination();
            LOG.info("Data source '" + dataSource.getName() + "' stopped in " + (Common.timer.currentTimeMillis() - now) + "ms");
        }catch(Exception e){
            LOG.error("Data source '" + dataSource.getName() + "' failed proper termination.", e);
        }
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
     * @param vo
     * @param latestValue
     */
    @Override
    public void startDataPointStartup(DataPointWithEventDetectorsAndCache vo) {
        Assert.isTrue(vo.getDataPoint().isEnabled(), "Data point not enabled");

        // Only add the data point if its data source is enabled.
        DataSourceRT<? extends DataSourceVO> ds = getRunningDataSource(vo.getDataPoint().getDataSourceId());
        if (ds != null) {
            // Change the VO into a data point implementation.
            DataPointRT dataPoint = new DataPointRT(vo, vo.getDataPoint().getPointLocator().createRuntime(), ds.getVo(), vo.getInitialCache());

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
            dataPoint.initialize();

            //If we are a polling data source then we need to wait to start our interval logging
            // until the first poll due to quantization
            boolean isPolling = ds instanceof PollingDataSource;

            //If we are not polling go ahead and start the interval logging, otherwise we will let the data source do it on the first poll
            if(!isPolling)
                dataPoint.initializeIntervalLogging(0l, false);

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
                DataPointDao.getInstance().saveEnabledColumn(dataPoint.getVO());
                stopDataPoint(dataPoint.getId()); //Stop it
            }
        }
    }

    /**
     * Only to be used at shutdown as synchronization has been reduced for performance
     */
    private void stopDataPointShutdown(DataPointVO dp) {

        DataPointRT p = null;
        synchronized (dataPoints) {
            // Remove this point from the data image if it is there. If not, just quit.
            p = dataPoints.remove(dp.getId());
        }
        // Remove it from the data source, and terminate it.
        if (p != null) {
            try{
                getRunningDataSource(p.getDataSourceId()).removeDataPoint(p);
            }catch(Exception e){
                LOG.error("Failed to stop point RT with ID: " + dp.getId()
                + " stopping point."
                , e);
            }
            DataPointListener l = getDataPointListeners(dp.getId());
            if (l != null)
                try {
                    l.pointTerminated(dp);
                } catch(ExceptionListWrapper e) {
                    LOG.warn("Exceptions in point terminated method.");
                    for(Exception e2 : e.getExceptions())
                        LOG.warn("Listener exception: " + e2.getMessage(), e2);
                }
            //Stop data point but don't cancel events until after to do this in bulk.
            p.terminate();
        }

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
        dataPointListeners.compute(dataPointId, (k, v) -> {
            return DataPointEventMulticaster.add(v, l);
        });
    }

    @Override
    public void removeDataPointListener(int dataPointId, DataPointListener l) {
        dataPointListeners.compute(dataPointId, (k, v) -> {
            return DataPointEventMulticaster.remove(v, l);
        });
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
        // The data source may have been disabled. Just make sure.
        if (ds != null)
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
            throw new RTException("Point is not relinquishable");

        // Tell the data source to relinquish value of the point.
        DataSourceRT<? extends DataSourceVO> ds = getRunningDataSource(dataPoint.getDataSourceId());
        // The data source may have been disabled. Just make sure.
        if (ds != null)
            ds.relinquish(dataPoint);
    }

    @Override
    public void forcePointRead(int dataPointId) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        // Tell the data source to read the point value;
        DataSourceRT<? extends DataSourceVO> ds = getRunningDataSource(dataPoint.getDataSourceId());
        if (ds != null)
            // The data source may have been disabled. Just make sure.
            ds.forcePointRead(dataPoint);
    }

    @Override
    public void forceDataSourcePoll(int dataSourceId) {
        DataSourceRT<? extends DataSourceVO> dataSource = runningDataSources.get(dataSourceId);
        if(dataSource == null)
            throw new RTException("Source is not enabled");

        dataSource.forcePoll();
    }

    @Override
    public long purgeDataPointValues() {
        long count = Common.databaseProxy.newPointValueDao().deleteAllPointData();
        for (Integer id : dataPoints.keySet())
            updateDataPointValuesRT(id, Long.MAX_VALUE);
        return count;
    }

    @Override
    public void purgeDataPointValuesWithoutCount() {
        Common.databaseProxy.newPointValueDao().deleteAllPointDataWithoutCount();
        for (Integer id : dataPoints.keySet())
            updateDataPointValuesRT(id, Long.MAX_VALUE);
        return;
    }

    @Override
    public long purgeDataPointValues(DataPointVO vo, int periodType, int periodCount) {
        long before = DateUtils.minus(Common.timer.currentTimeMillis(), periodType, periodCount);
        return purgeDataPointValues(vo, before);
    }

    @Override
    public long purgeDataPointValues(DataPointVO vo) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValues(vo);
        updateDataPointValuesRT(vo.getId(), Long.MAX_VALUE);
        return count;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(DataPointVO vo) {
        if(Common.databaseProxy.newPointValueDao().deletePointValuesWithoutCount(vo)){
            updateDataPointValuesRT(vo.getId(), Long.MAX_VALUE);
            return true;
        }else
            return false;
    }

    @Override
    public long purgeDataPointValue(DataPointVO vo, long ts, PointValueDao dao){
        long count = dao.deletePointValue(vo, ts);
        if(count > 0)
            updateDataPointValuesRT(vo.getId());
        return count;

    }

    @Override
    public long purgeDataPointValues(DataPointVO vo, long before) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValuesBefore(vo, before);
        if (count > 0)
            updateDataPointValuesRT(vo.getId(), before);
        return count;
    }

    @Override
    public long purgeDataPointValuesBetween(DataPointVO vo, long startTime, long endTime) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValuesBetween(vo, startTime, endTime);
        if(count > 0)
            updateDataPointValuesRT(vo.getId(), endTime);
        return count;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(DataPointVO vo, long before) {
        if(Common.databaseProxy.newPointValueDao().deletePointValuesBeforeWithoutCount(vo, before)){
            updateDataPointValuesRT(vo.getId(), before);
            return true;
        }else
            return false;
    }


    private void updateDataPointValuesRT(int dataPointId) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint != null)
            // Enabled. Reset the point's cache.
            dataPoint.resetValues();
    }

    private void updateDataPointValuesRT(int dataPointId, long before) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint != null)
            // Enabled. Reset the point's cache.
            dataPoint.resetValues(before);
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
        return PublisherDao.getInstance().get(publisherId);
    }

    @Override
    public void deletePublisher(int publisherId) {
        stopPublisher(publisherId);
        PublisherDao.getInstance().delete(publisherId);
        Common.eventManager.cancelEventsForPublisher(publisherId);
    }

    @Override
    public void savePublisher(PublisherVO<? extends PublishedPointVO> vo) {
        // If the publisher is running, stop it.
        stopPublisher(vo.getId());

        // In case this is a new publisher, we need to save to the database first so that it has a proper id.
        if(vo.getId() == Common.NEW_ID) {
            PublisherDao.getInstance().insert(vo);
        }else {
            PublisherDao.getInstance().update(vo.getId(), vo);
        }

        // If the publisher is enabled, start it.
        if (vo.isEnabled())
            startPublisher(vo);
    }

    private void startPublisher(PublisherVO<? extends PublishedPointVO> vo) {
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

    private void stopPublisher(int id) {
        synchronized (runningPublishers) {
            PublisherRT<?> publisher = getRunningPublisher(id);
            if (publisher == null)
                return;

            publisher.terminate();
            publisher.joinTermination();
            runningPublishers.remove(publisher);
        }
    }
}
