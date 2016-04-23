/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EnhancedPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
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
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

public class RuntimeManager {
    private static final Log LOG = LogFactory.getLog(RuntimeManager.class);

    private final Map<Integer, DataSourceRT> runningDataSources = new ConcurrentHashMap<Integer, DataSourceRT>();

    /**
     * Provides a quick lookup map of the running data points.
     */
    private final Map<Integer, DataPointRT> dataPoints = new ConcurrentHashMap<Integer, DataPointRT>();

    /**
     * The list of point listeners, kept here such that listeners can be notified of point initializations (i.e. a
     * listener can register itself before the point is enabled).
     */
    private final Map<Integer, DataPointListener> dataPointListeners = new ConcurrentHashMap<Integer, DataPointListener>();

    /**
     * Store of enabled publishers
     */
    private final List<PublisherRT<?>> runningPublishers = new CopyOnWriteArrayList<PublisherRT<?>>();

    private boolean started = false;

    //
    // Lifecycle
    synchronized public void initialize(boolean safe) {
        if (started)
            throw new ShouldNeverHappenException("RuntimeManager already started");

        // Set the started indicator to true.
        started = true;
        
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
        DataSourceDao dataSourceDao = DaoRegistry.dataSourceDao;
        List<DataSourceVO<?>> configs = dataSourceDao.getDataSources();
        Map<DataSourceDefinition.StartPriority, List<DataSourceVO<?>>> priorityMap = new HashMap<DataSourceDefinition.StartPriority, List<DataSourceVO<?>>>();
        for (DataSourceVO<?> config : configs) {
            if (config.isEnabled()) {
                if (safe) {
                    config.setEnabled(false);
                    dataSourceDao.saveDataSource(config);
                }
                else if (config.getDefinition() != null) {
                    List<DataSourceVO<?>> priorityList = priorityMap.get(config.getDefinition().getStartPriority());
                    if (priorityList == null) {
                        priorityList = new ArrayList<DataSourceVO<?>>();
                        priorityMap.put(config.getDefinition().getStartPriority(), priorityList);
                    }
                    priorityList.add(config);
                }
            }
        }

        // Initialize the prioritized data sources. Start the polling later.
        List<DataSourceVO<?>> pollingRound = new ArrayList<DataSourceVO<?>>();
        int dataSourceStartupThreads = Common.envProps.getInt("runtime.datasource.startupThreads", 8);
        boolean useMetrics = Common.envProps.getBoolean("runtime.datasource.logStartupMetrics", false);
        for (DataSourceDefinition.StartPriority startPriority : DataSourceDefinition.StartPriority.values()) {
            List<DataSourceVO<?>> priorityList = priorityMap.get(startPriority);
            if (priorityList != null) {
            	DataSourceGroupInitializer initializer = new DataSourceGroupInitializer(startPriority, priorityList, useMetrics, dataSourceStartupThreads);
            	pollingRound.addAll(initializer.initialize());
            }
        }

        // Tell the data sources to start polling. Delaying the polling start gives the data points a chance to
        // initialize such that point listeners in meta points and set point handlers can run properly.
        for (DataSourceVO<?> config : pollingRound)
            startDataSourcePolling(config);

        // Run everything else.
        rtmdIndex = startRTMDefs(defs, safe, rtmdIndex, Integer.MAX_VALUE);

        // Start the publishers that are enabled
        PublisherDao publisherDao = DaoRegistry.publisherDao;
        List<PublisherVO<? extends PublishedPointVO>> publishers = publisherDao.getPublishers();
        for (PublisherVO<? extends PublishedPointVO> vo : publishers) {
            if (vo.isEnabled()) {
                if (safe) {
                    vo.setEnabled(false);
                    publisherDao.savePublisher(vo);
                }
                else
                    startPublisher(vo);
            }
        }
        
        //Schedule the Backup Tasks if necessary
        // No way to set the default value for Bools in SystemSettingsDao so must do here
        if(!safe){
	        if(SystemSettingsDao.getBooleanValue(SystemSettingsDao.BACKUP_ENABLED,true)){
	       		BackupWorkItem.schedule();
	        }
	        if(SystemSettingsDao.getBooleanValue(SystemSettingsDao.DATABASE_BACKUP_ENABLED,true)){
	       		DatabaseBackupWorkItem.schedule();
	        }

        }
        
    }

    synchronized public void terminate() {
        if (!started)
            throw new ShouldNeverHappenException("RuntimeManager not yet started");

        started = false;

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
        Map<DataSourceDefinition.StartPriority, List<DataSourceRT>> priorityMap = new HashMap<DataSourceDefinition.StartPriority, List<DataSourceRT>>();
        for (Entry<Integer, DataSourceRT> entry : runningDataSources.entrySet()) {
            DataSourceRT rt = entry.getValue();
            List<DataSourceRT> priorityList = priorityMap.get(rt.getVo().getDefinition().getStartPriority());
            if (priorityList == null) {
                priorityList = new ArrayList<DataSourceRT>();
                priorityMap.put(rt.getVo().getDefinition().getStartPriority(), priorityList);
            }
            priorityList.add(rt);
        }

        int dataSourceStartupThreads = Common.envProps.getInt("runtime.datasource.startupThreads", 8);
        boolean useMetrics = Common.envProps.getBoolean("runtime.datasource.logStartupMetrics", false);
        DataSourceDefinition.StartPriority[] priorities = DataSourceDefinition.StartPriority.values();
        for (int i = priorities.length - 1; i >= 0; i--) {
            List<DataSourceRT> priorityList = priorityMap.get(priorities[i]);
            if (priorityList != null) {
            	DataSourceGroupTerminator initializer = new DataSourceGroupTerminator(priorities[i], priorityList, useMetrics, dataSourceStartupThreads);
                initializer.terminate();
            }
        }

        // Run everything else.
        rtmdIndex = stopRTMDefs(defs, rtmdIndex, Integer.MIN_VALUE);
    }

    public void joinTermination() {
        for (Entry<Integer, DataSourceRT> entry : runningDataSources.entrySet()) {
            DataSourceRT dataSource = entry.getValue();
            try {
                dataSource.joinTermination();
            }
            catch (ShouldNeverHappenException e) {
                LOG.error("Error stopping data source " + dataSource.getId(), e);
            }
        }
    }

    private int startRTMDefs(List<RuntimeManagerDefinition> defs, boolean safe, int fromIndex, int toPriority) {
        while (fromIndex < defs.size() && defs.get(fromIndex).getInitializationPriority() <= toPriority)
            defs.get(fromIndex++).initialize(safe);
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
    public DataSourceRT getRunningDataSource(int dataSourceId) {
        return runningDataSources.get(dataSourceId);
    }

    public boolean isDataSourceRunning(int dataSourceId) {
        return getRunningDataSource(dataSourceId) != null;
    }

    public List<DataSourceVO<?>> getDataSources() {
        return new DataSourceDao().getDataSources();
    }

    public DataSourceVO<?> getDataSource(int dataSourceId) {
        return new DataSourceDao().getDataSource(dataSourceId);
    }

    public void deleteDataSource(int dataSourceId) {
        stopDataSource(dataSourceId);
        new DataSourceDao().deleteDataSource(dataSourceId);
        Common.eventManager.cancelEventsForDataSource(dataSourceId);
    }

    public void saveDataSource(DataSourceVO<?> vo) {
        // If the data source is running, stop it.
        stopDataSource(vo.getId());

        // In case this is a new data source, we need to save to the database first so that it has a proper id.
        DaoRegistry.dataSourceDao.saveDataSource(vo);

        // If the data source is enabled, start it.
        if (vo.isEnabled()) {
            if (initializeDataSource(vo))
                startDataSourcePolling(vo);
        }
    }

    private boolean initializeDataSource(DataSourceVO<?> vo) {
        synchronized (runningDataSources) {
            return initializeDataSourceStartup(vo);
        }
    }

    /**
     * Only to be used at startup as the synchronization has been reduced for performance
     * @param vo
     * @return
     */
    boolean initializeDataSourceStartup(DataSourceVO<?> vo) {
    	long startTime = System.nanoTime();

        // If the data source is already running, just quit.
        if (isDataSourceRunning(vo.getId()))
            return false;

        // Ensure that the data source is enabled.
        Assert.isTrue(vo.isEnabled());

        // Create and initialize the runtime version of the data source.
        DataSourceRT dataSource = vo.createDataSourceRT();
        dataSource.initialize();

        // Add it to the list of running data sources.
        synchronized(runningDataSources) {
        	runningDataSources.put(dataSource.getId(), dataSource);
        }

        // Add the enabled points to the data source.
        List<DataPointVO> dataSourcePoints = DaoRegistry.dataPointDao.getDataPointsForDataSourceStart(vo.getId());
        
        Map<Integer, List<PointValueTime>> latestValuesMap = null;
        if (DaoRegistry.pointValueDao instanceof EnhancedPointValueDao) {
            EnhancedPointValueDao pvDao = (EnhancedPointValueDao) DaoRegistry.pointValueDao;
            
            // XXX Find the maximum cache size for all point in the datasource
            // This number of values will be retrieved for all points in the datasource
            // If even one point has a high cache size this *may* cause issues
            int maxCacheSize = 0;
            for (DataPointVO dataPoint : dataSourcePoints) {
                if (dataPoint.getDefaultCacheSize() > maxCacheSize)
                    maxCacheSize = dataPoint.getDefaultCacheSize();
            }
            
            try {
                latestValuesMap = pvDao.getLatestPointValuesForDataSource(vo, maxCacheSize);
            } catch (Exception e) {
                LOG.error("Failed to get latest point values for datasource " + vo.getXid() + ". Mango will try to retrieve latest point values per point which will take longer.", e);
            }
        }
        
        for (DataPointVO dataPoint : dataSourcePoints) {
            if (dataPoint.isEnabled()) {
                List<PointValueTime> latestValuesForPoint = null;
                if (latestValuesMap != null) {
                    latestValuesForPoint = latestValuesMap.get(dataPoint.getId());
                    if (latestValuesForPoint == null) {
                        latestValuesForPoint = new ArrayList<>();
                    }
                }
                try {
                    startDataPointStartup(dataPoint, latestValuesForPoint);
                } catch (Exception e) {
                    LOG.error("Failed to start data point " + dataPoint.getXid(), e);
                }
            }
        }

        LOG.info("Data source '" + vo.getName() + "' initialized");

    	long endTime = System.nanoTime();

    	long duration = endTime - startTime;
    	LOG.info("Data source '" + vo.getName() + "' took " + (double)duration/(double)1000000 + "ms to start");
        return true;
    }
    
    private void startDataSourcePolling(DataSourceVO<?> vo) {
        DataSourceRT dataSource = getRunningDataSource(vo.getId());
        if (dataSource != null)
            dataSource.beginPolling();
    }

    private void stopDataSource(int id) {
        synchronized (runningDataSources) {

            DataSourceRT dataSource = getRunningDataSource(id);
            if (dataSource == null)
                return;
	        try{	
	            // Stop the data points.
	            for (DataPointRT p : dataPoints.values()) {
	                if (p.getDataSourceId() == id)
	                    stopDataPoint(p.getId());
	            }
	
	            runningDataSources.remove(dataSource.getId());
	            dataSource.terminate();
	
	            dataSource.joinTermination();
	            LOG.info("Data source '" + dataSource.getName() + "' stopped");
        	}catch(Exception e){
        		LOG.error("Data source '" + dataSource.getName() + "' failed proper termination.", e);
        	}
        }
    }
    
    /**
     * Should only be called at Shutdown as synchronization has been reduced for performance
     */
    void stopDataSourceShutdown(int id) {

        DataSourceRT dataSource = getRunningDataSource(id);
        if (dataSource == null)
            return;
        try{	
            // Stop the data points.
            for (DataPointRT p : dataPoints.values()) {
                if (p.getDataSourceId() == id)
                    stopDataPointShutdown(p.getId());
            }
            synchronized (runningDataSources) {
            	runningDataSources.remove(dataSource.getId());
            }
            
            dataSource.terminate();

            dataSource.joinTermination();
            LOG.info("Data source '" + dataSource.getName() + "' stopped");
    	}catch(Exception e){
    		LOG.error("Data source '" + dataSource.getName() + "' failed proper termination.", e);
    	}
    }

    //
    //
    // Data points
    //
    public void saveDataPoint(DataPointVO point) {
        stopDataPoint(point.getId());

        // Since the point's data type may have changed, we must ensure that the other attrtibutes are still ok with
        // it.
        int dataType = point.getPointLocator().getDataTypeId();

        // Chart renderer
        if (point.getChartRenderer() != null && !point.getChartRenderer().getDef().supports(dataType))
            // Return to a default renderer
            point.setChartRenderer(null);

        // Text renderer
        if (point.getTextRenderer() != null && !point.getTextRenderer().getDef().supports(dataType))
            // Return to a default renderer
            point.defaultTextRenderer();

        // Event detectors
        Iterator<AbstractPointEventDetectorVO<?>> peds = point.getEventDetectors().iterator();
        while (peds.hasNext()) {
        	AbstractPointEventDetectorVO<?> ped = peds.next();
            if (!ped.supports(dataType))
                // Remove the detector.
                peds.remove();
        }

        DaoRegistry.dataPointDao.saveDataPoint(point);

        if (point.isEnabled())
            startDataPoint(point, null);
    }

    public void deleteDataPoint(DataPointVO point) {
        if (point.isEnabled())
            stopDataPoint(point.getId());
        new DataPointDao().deleteDataPoint(point.getId());
        Common.eventManager.cancelEventsForDataPoint(point.getId());
    }

    private void startDataPoint(DataPointVO vo, List<PointValueTime> initialCache) {
        synchronized (dataPoints) {
            startDataPointStartup(vo, initialCache);
        }
    }

    /**
     * Only to be used at startup as synchronization has been reduced for performance
     * @param vo
     * @param latestValue 
     */
    private void startDataPointStartup(DataPointVO vo, List<PointValueTime> initialCache) {
        Assert.isTrue(vo.isEnabled());

        // Only add the data point if its data source is enabled.
        DataSourceRT ds = getRunningDataSource(vo.getDataSourceId());
        if (ds != null) {
            // Change the VO into a data point implementation.
            DataPointRT dataPoint = new DataPointRT(vo, vo.getPointLocator().createRuntime(), ds.getVo(), initialCache);

            // Add/update it in the data image.
            synchronized (dataPoints) {
            	dataPoints.put(dataPoint.getId(), dataPoint);
            }

            // Initialize it.
            dataPoint.initialize();
            
            //If we are a polling data source then we need to wait to start our interval logging
            // until the first poll due to quantization
            boolean isPolling = ds instanceof PollingDataSource;
            
            //If we are not polling go ahead and start the interval logging, otherwise we will let the data source do it on the first poll
            if(!isPolling)
            	dataPoint.initializeIntervalLogging(0l, false);
            
            DataPointListener l = getDataPointListeners(vo.getId());
            if (l != null)
                l.pointInitialized();

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
            	//Common.eventManager.raiseEvent(type, time, rtnApplicable, alarmLevel, message, context);
            	dataPoint.getVO().setEnabled(false);
            	saveDataPoint(dataPoint.getVO()); //Stop it
            }
        }
    }
    
    private void stopDataPoint(int dataPointId) {
        synchronized (dataPoints) {
            // Remove this point from the data image if it is there. If not, just quit.
            DataPointRT p = dataPoints.remove(dataPointId);

            // Remove it from the data source, and terminate it.
            if (p != null) {
            	try{
            		getRunningDataSource(p.getDataSourceId()).removeDataPoint(p);
            	}catch(Exception e){
            		LOG.error("Failed to stop point RT with ID: " + dataPointId
                			+ " stopping point."
                			, e);
            	}
                DataPointListener l = getDataPointListeners(dataPointId);
                if (l != null)
                    l.pointTerminated();
                p.terminate();
            }
        }
    }

    /**
     * Only to be used at shutdown as synchronization has been reduced for performance
     */
    private void stopDataPointShutdown(int dataPointId) {
        
    	DataPointRT p = null;
    	synchronized (dataPoints) {
            // Remove this point from the data image if it is there. If not, just quit.
            p = dataPoints.remove(dataPointId);
    	}
        // Remove it from the data source, and terminate it.
        if (p != null) {
        	try{
        		getRunningDataSource(p.getDataSourceId()).removeDataPoint(p);
        	}catch(Exception e){
        		LOG.error("Failed to stop point RT with ID: " + dataPointId
            			+ " stopping point."
            			, e);
        	}
            DataPointListener l = getDataPointListeners(dataPointId);
            if (l != null)
                l.pointTerminated();
            p.terminate();
        }

    }
    
    public void restartDataPoint(DataPointVO vo){
    	this.stopDataPoint(vo.getId());
    	this.startDataPoint(vo, null);
    }
    
    public boolean isDataPointRunning(int dataPointId) {
        return dataPoints.get(dataPointId) != null;
    }

    public DataPointRT getDataPoint(int dataPointId) {
        return dataPoints.get(dataPointId);
    }

    public void addDataPointListener(int dataPointId, DataPointListener l) {
        DataPointListener listeners = dataPointListeners.get(dataPointId);
        dataPointListeners.put(dataPointId, DataPointEventMulticaster.add(listeners, l));
    }

    public void removeDataPointListener(int dataPointId, DataPointListener l) {
        DataPointListener listeners = DataPointEventMulticaster.remove(dataPointListeners.get(dataPointId), l);
        if (listeners == null)
            dataPointListeners.remove(dataPointId);
        else
            dataPointListeners.put(dataPointId, listeners);
    }

    public DataPointListener getDataPointListeners(int dataPointId) {
        return dataPointListeners.get(dataPointId);
    }

    //
    // Point values
    public void setDataPointValue(int dataPointId, DataValue value, SetPointSource source) {
        setDataPointValue(dataPointId, new PointValueTime(value, System.currentTimeMillis()), source);
    }

    public void setDataPointValue(int dataPointId, PointValueTime valueTime, SetPointSource source) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        if (!dataPoint.getPointLocator().isSettable())
            throw new RTException("Point is not settable");

        // Tell the data source to set the value of the point.
        DataSourceRT ds = getRunningDataSource(dataPoint.getDataSourceId());
        // The data source may have been disabled. Just make sure.
        if (ds != null)
            ds.setPointValue(dataPoint, valueTime, source);
    }

    public void relinquish(int dataPointId) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        if (!dataPoint.getPointLocator().isSettable())
            throw new RTException("Point is not settable");
        if (!dataPoint.getPointLocator().isRelinquishable())
            throw new RTException("Point is not relinquishable");

        // Tell the data source to relinquish value of the point.
        DataSourceRT ds = getRunningDataSource(dataPoint.getDataSourceId());
        // The data source may have been disabled. Just make sure.
        if (ds != null)
            ds.relinquish(dataPoint);
    }

    /**
     * This method forces a point read ONLY if the 
     * underlying data source has implemented that ability.
     * 
     * Currently only a few data sources implement this functionality
     * @param dataPointId
     */
    public void forcePointRead(int dataPointId) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint == null)
            throw new RTException("Point is not enabled");

        // Tell the data source to read the point value;
        DataSourceRT ds = getRunningDataSource(dataPoint.getDataSourceId());
        if (ds != null)
            // The data source may have been disabled. Just make sure.
            ds.forcePointRead(dataPoint);
    }

    public long purgeDataPointValues() {
        PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();
        long count = pointValueDao.deleteAllPointData();
        for (Integer id : dataPoints.keySet())
            updateDataPointValuesRT(id);
        return count;
    }

    public long purgeDataPointValues(int dataPointId, int periodType, int periodCount) {
        long before = DateUtils.minus(System.currentTimeMillis(), periodType, periodCount);
        return purgeDataPointValues(dataPointId, before);
    }

    public long purgeDataPointValues(int dataPointId) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValues(dataPointId);
        updateDataPointValuesRT(dataPointId);
        return count;
    }
    
    /**
     * Purge a value at a given time
     * @param dataPointId
     * @param ts
     * @return
     */
    public long purgeDataPointValue(int dataPointId, long ts){
    	long count = Common.databaseProxy.newPointValueDao().deletePointValue(dataPointId, ts);
    	if(count > 0)
    		updateDataPointValuesRT(dataPointId);
    	return count;
    	
    }

    public long purgeDataPointValues(int dataPointId, long before) {
        long count = Common.databaseProxy.newPointValueDao().deletePointValuesBefore(dataPointId, before);
        if (count > 0)
            updateDataPointValuesRT(dataPointId);
        return count;
    }

    private void updateDataPointValuesRT(int dataPointId) {
        DataPointRT dataPoint = dataPoints.get(dataPointId);
        if (dataPoint != null)
            // Enabled. Reset the point's cache.
            dataPoint.resetValues();
    }

    //
    //
    // Publishers
    //
    public PublisherRT<?> getRunningPublisher(int publisherId) {
        for (PublisherRT<?> publisher : runningPublishers) {
            if (publisher.getId() == publisherId)
                return publisher;
        }
        return null;
    }

    public boolean isPublisherRunning(int publisherId) {
        return getRunningPublisher(publisherId) != null;
    }

    public PublisherVO<? extends PublishedPointVO> getPublisher(int publisherId) {
        return new PublisherDao().getPublisher(publisherId);
    }

    public void deletePublisher(int publisherId) {
        stopPublisher(publisherId);
        new PublisherDao().deletePublisher(publisherId);
        Common.eventManager.cancelEventsForPublisher(publisherId);
    }

    public void savePublisher(PublisherVO<? extends PublishedPointVO> vo) {
        // If the data source is running, stop it.
        stopPublisher(vo.getId());

        // In case this is a new publisher, we need to save to the database first so that it has a proper id.
        new PublisherDao().savePublisher(vo);

        // If the publisher is enabled, start it.
        if (vo.isEnabled())
            startPublisher(vo);
    }

    private void startPublisher(PublisherVO<? extends PublishedPointVO> vo) {
        synchronized (runningPublishers) {
            // If the publisher is already running, just quit.
            if (isPublisherRunning(vo.getId()))
                return;

            // Ensure that the data source is enabled.
            Assert.isTrue(vo.isEnabled());

            // Create and start the runtime version of the publisher.
            PublisherRT<?> publisher = vo.createPublisherRT();
            publisher.initialize();

            // Add it to the list of running publishers.
            runningPublishers.add(publisher);
        }
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
