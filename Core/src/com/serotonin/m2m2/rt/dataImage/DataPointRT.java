/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EnhancedPointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.rt.script.AbstractPointWrapper;
import com.serotonin.m2m2.rt.script.DataPointWrapper;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.view.stats.AnalogStatistics;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycle;

public final class DataPointRT implements IDataPointValueSource, ILifecycle {
    private static final Log LOG = LogFactory.getLog(DataPointRT.class);
    private static final PvtTimeComparator pvtTimeComparator = new PvtTimeComparator();
    private static final String prefix = "INTVL_LOG-";
    private static final boolean enhanced = Common.databaseProxy.newPointValueDao() instanceof EnhancedPointValueDao;

    // Configuration data.
    private final DataPointVO vo;
    private final DataSourceVO<?> dsVo;
    private final PointLocatorRT<?> pointLocator;

    // Runtime data.
    private volatile PointValueTime pointValue;
    private final PointValueCache valueCache;
    private List<PointEventDetectorRT<?>> detectors;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    // Interval logging data.
    private PointValueTime intervalValue;
    private long intervalStartTime = -1;
    private List<IValueTime> averagingValues;
    private final Object intervalLoggingLock = new Object();
    private TimerTask intervalLoggingTask;

    //Simulation Timer, or any timer implementation
    //TODO Remove references to Common.timer and pass into constructor?
    private AbstractTimer timer;
    
    /**
     * This is the value around which tolerance decisions will be made when determining whether to log numeric values.
     */
    private double toleranceOrigin;

    public DataPointRT(DataPointVO vo, PointLocatorRT<?> pointLocator, DataSourceVO<?> dsVo, List<PointValueTime> initialCache) {
        this.vo = vo;
        this.dsVo = dsVo;
        this.pointLocator = pointLocator;
        if (enhanced) {
            valueCache = new EnhancedPointValueCache(vo, dsVo, vo.getDefaultCacheSize(), initialCache);
        } else {
            valueCache = new PointValueCache(vo.getId(), vo.getDefaultCacheSize(), initialCache);
        }
        if(vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.AVERAGE)
        	averagingValues = new ArrayList<IValueTime>();
    }

    /**
     * To allow simulation of points using a timer implementation
     * @param vo
     * @param pointLocator
     * @param Data Source
     * @param initial cache
     * @param timer
     */
    public DataPointRT(DataPointVO vo, PointLocatorRT<?> pointLocator, DataSourceVO<?> dsVo, List<PointValueTime> initialCache, AbstractTimer timer) {
        this(vo, pointLocator, dsVo, initialCache);
        this.timer = timer;
    }
    
    /**
     * For Legacy compatibility in 2.7.12
     * 
     * TODO Remove in 2.7.8 and fix modules
     * 
	 * @param DataPoint
	 * @param Point Locator RT
	 */
	public DataPointRT(DataPointVO dp, PointLocatorRT<?> rt) {
		this(dp, rt, DataSourceDao.instance.get(dp.getDataSourceId()), null);
	}

    /**
     * For Legacy compatibility in 2.7.12
     * 
     * TODO Remove in 2.7.8 and fix modules
     * 
     * To allow simulation of points using a timer implementation
     * @param vo
     * @param pointLocator
     * @param timer
     */
    public DataPointRT(DataPointVO dp, PointLocatorRT<?> pointLocator, AbstractTimer timer) {
        this(dp, pointLocator, DataSourceDao.instance.get(dp.getDataSourceId()), null);
        this.timer = timer;
    }
	
	//
    //
    // Single value
    //
    @Override
    public PointValueTime getPointValueBefore(long time) {
        for (PointValueTime pvt : valueCache.getCacheContents()) {
            if (pvt.getTime() < time)
                return pvt;
        }

        return Common.databaseProxy.newPointValueDao().getPointValueBefore(vo.getId(), time);
    }

    @Override
    public PointValueTime getPointValueAt(long time) {
        for (PointValueTime pvt : valueCache.getCacheContents()) {
            if (pvt.getTime() == time)
                return pvt;
        }

        return Common.databaseProxy.newPointValueDao().getPointValueAt(vo.getId(), time);
    }

    @Override
    public PointValueTime getPointValueAfter(long time) {
    	
    	//Get the value stored in the db
    	PointValueTime after = Common.databaseProxy.newPointValueDao().getPointValueAfter(vo.getId(), time);
        
    	//Check it with the cache
    	if(after != null){
    		List<PointValueTime> pvts = valueCache.getCacheContents();
    		//Check to see if we have a value closer in cache
	        for (int i = pvts.size() - 1; i >= 0; i--) {
	            PointValueTime pvt = pvts.get(i);
	            if (pvt.getTime() < after.getTime())
	                return pvt;
	        }
    	}else{
    		List<PointValueTime> pvts = valueCache.getCacheContents();
	        for (int i = pvts.size() - 1; i >= 0; i--) {
	            PointValueTime pvt = pvts.get(i);
	            if (pvt.getTime() >= time)
	                return pvt;
	        }
    	}
    	
        return after;
    }

    //
    //
    // Value lists
    //
    @Override
    public List<PointValueTime> getLatestPointValues(int limit) {
        return valueCache.getLatestPointValues(limit);
    }
    
    @Override
    public List<PointValueTime> getPointValues(long since) {
        List<PointValueTime> result = Common.databaseProxy.newPointValueDao().getPointValues(vo.getId(), since);

        for (PointValueTime pvt : valueCache.getCacheContents()) {
            if (pvt.getTime() >= since) {
                int index = Collections.binarySearch(result, pvt, pvtTimeComparator);
                if (index < 0)
                    result.add(-index - 1, pvt);
            }
        }

        return result;
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(long from, long to) {
        List<PointValueTime> result = Common.databaseProxy.newPointValueDao().getPointValuesBetween(vo.getId(), from, to);

        for (PointValueTime pvt : valueCache.getCacheContents()) {
            if (pvt.getTime() >= from && pvt.getTime() < to) {
                int index = Collections.binarySearch(result, pvt, pvtTimeComparator);
                if (index < 0)
                    result.add(-index - 1, pvt);
            }
        }

        return result;
    }

    /**
     * This method should only be called by the data source. Other types of point setting should include a set point
     * source object so that the annotation can be logged.
     * 
     * @param newValue
     */
    @Override
    public void updatePointValue(PointValueTime newValue) {
        savePointValue(newValue, null, true, true);
    }

    @Override
    public void updatePointValue(PointValueTime newValue, boolean async) {
        savePointValue(newValue, null, async, true);
    }
    

    public void updatePointValue(PointValueTime newValue, boolean async, boolean saveToDatabase) {
        savePointValue(newValue, null, async, saveToDatabase);
    }
    
    /**
     * Use this method to update a data point for reasons other than just data source update.
     * 
     * @param newValue
     *            the value to set
     * @param source
     *            the source of the set. This can be a user object if the point was set from the UI, or could be a
     *            program run by schedule or on event.
     */
    @Override
    public void setPointValue(PointValueTime newValue, SetPointSource source) {
        if (source == null)
            savePointValue(newValue, source, true, true);
        else
            savePointValue(newValue, source, false, true);
    }

    private void savePointValue(PointValueTime newValue, SetPointSource source, boolean async, boolean saveToDatabase) {
        // Null values are not very nice, and since they don't have a specific meaning they are hereby ignored.
        if (newValue == null)
            return;

        // Check the data type of the value against that of the locator, just for fun.
        int valueDataType = DataTypes.getDataType(newValue.getValue());
        if (valueDataType != DataTypes.UNKNOWN && valueDataType != vo.getPointLocator().getDataTypeId())
            // This should never happen, but if it does it can have serious downstream consequences. Also, we need
            // to know how it happened, and the stack trace here provides the best information.
            throw new ShouldNeverHappenException("Data type mismatch between new value and point locator: newValue="
                    + DataTypes.getDataType(newValue.getValue()) + ", locator=" + vo.getPointLocator().getDataTypeId());

        // Check if this value qualifies for discardation.
        if (vo.isDiscardExtremeValues() && DataTypes.getDataType(newValue.getValue()) == DataTypes.NUMERIC) {
            double newd = newValue.getDoubleValue();
            //Discard if NaN
            if(Double.isNaN(newd))
            	return;
            
            if (newd < vo.getDiscardLowLimit() || newd > vo.getDiscardHighLimit())
                // Discard the value
                return;
        }

        if (newValue.getTime() > Common.timer.currentTimeMillis() + SystemSettingsDao.getFutureDateLimit()) {
            // Too far future dated. Toss it. But log a message first.
            LOG.warn("Future dated value detected: pointId=" + vo.getId() + ", value=" + newValue.getStringValue()
                    + ", type=" + vo.getPointLocator().getDataTypeId() + ", ts=" + newValue.getTime(), new Exception());
            return;
        }

        boolean backdated = pointValue != null && newValue.getTime() < pointValue.getTime();

        // Determine whether the new value qualifies for logging.
        boolean logValue;
        // ... or even saving in the cache.
        boolean saveValue = true;
        switch (vo.getLoggingType()) {
        case DataPointVO.LoggingTypes.ON_CHANGE:
            if (pointValue == null)
                logValue = true;
            else if (backdated)
                // Backdated. Ignore it
                logValue = false;
            else {
                if (newValue.getValue() instanceof NumericValue) {
                    // Get the new double
                    double newd = newValue.getDoubleValue();

                    // See if the new value is outside of the tolerance.
                    double diff = toleranceOrigin - newd;
                    if (diff < 0)
                        diff = -diff;

                    if (diff > vo.getTolerance()) {
                        toleranceOrigin = newd;
                        logValue = true;
                    }
                    else
                        logValue = false;
                } else if(newValue.getValue() instanceof ImageValue) {
                    logValue = !((ImageValue)newValue.getValue()).equalDigests(((ImageValue)pointValue.getValue()).getDigest());
                } else
                    logValue = !ObjectUtils.equals(newValue.getValue(), pointValue.getValue());
            }

            saveValue = logValue;
            break;
        case DataPointVO.LoggingTypes.ALL:
            logValue = true;
            break;
        case DataPointVO.LoggingTypes.ON_TS_CHANGE:
            if (pointValue == null)
                logValue = true;
            else if (backdated)
                // Backdated. Ignore it
                logValue = false;
            else
                logValue = newValue.getTime() != pointValue.getTime();

            saveValue = logValue;
            break;
        case DataPointVO.LoggingTypes.INTERVAL:
            if (!backdated)
                intervalSave(newValue);
        default:
            logValue = false;
        }
        
        if(!saveToDatabase)
        	logValue = false;
        
        if (saveValue)
            valueCache.savePointValue(newValue, source, logValue, async);

        // add annotation to newValue before firing events so event detectors can
        // fetch the annotation
        if (source != null) {
            newValue = new AnnotatedPointValueTime(newValue.getValue(),
                    newValue.getTime(), source.getSetPointSourceMessage());
        }
        
        // Ignore historical values.
        if (pointValue == null || newValue.getTime() >= pointValue.getTime()) {
            PointValueTime oldValue = pointValue;
            pointValue = newValue;
            fireEvents(oldValue, newValue, source != null, false, logValue, true);
        }
        else
            fireEvents(null, newValue, false, true, logValue, false);
    }

    /**
     * @param newValue
     * @param source
     * @param logValue
     * @param async
     * 
     * This method is called by modules that have the potential to generate a rapid flow of values and backdates
     *  for the purpose of circumventing the update method's various controls on logging behaviors. It does not
     *  generate events because the expected rate of calls from the modules which use it is somewhere between high
     *  and very high.
     */
    public void savePointValueDirectToCache(PointValueTime newValue, SetPointSource source, boolean logValue,
            boolean async) {
    	 valueCache.savePointValue(newValue, source, logValue, async);
    }

    //
    // / Interval logging
    //
    /**
     * 
     */
    public void initializeIntervalLogging(long nextPollTime, boolean quantize) {
        synchronized (intervalLoggingLock) {
            if (vo.getLoggingType() != DataPointVO.LoggingTypes.INTERVAL)
                return;
            long delay = 0l;
            long loggingPeriodMillis = Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod());
            if(quantize){
            	// Quantize the start.
            	//Compute delay only if we are offset from the next poll time
            	long nextPollOffset = (nextPollTime % loggingPeriodMillis);
            	if(nextPollOffset != 0)
            		delay = loggingPeriodMillis - nextPollOffset;
                LOG.debug("First interval log should be at: " + (nextPollTime + delay));
            }
            //Are we using a custom timer?
            if(this.timer == null)
	            intervalLoggingTask = new TimeoutTask(new FixedRateTrigger(delay, loggingPeriodMillis), createIntervalLoggingTimeoutClient());
            else
	            intervalLoggingTask = new TimeoutTask(new FixedRateTrigger(delay, loggingPeriodMillis), createIntervalLoggingTimeoutClient(), this.timer);
            	
            intervalValue = pointValue;
            if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.AVERAGE) {
                intervalStartTime = Common.timer.currentTimeMillis();
                if(averagingValues.size() > 0) {
                	AnalogStatistics stats = new AnalogStatistics(intervalStartTime-loggingPeriodMillis, intervalStartTime,
                			null, averagingValues, intervalValue);
                	PointValueTime newValue = new PointValueTime(stats.getAverage(), intervalStartTime);
                    valueCache.logPointValueAsync(newValue, null);
                    //Fire logged Events
                    fireEvents(null, newValue, false, false, true, false);
                	averagingValues.clear();
                }
            }
        }
    }
    
    private TimeoutClient createIntervalLoggingTimeoutClient(){
        return  new TimeoutClient(){

			@Override
			public void scheduleTimeout(long fireTime) {
				scheduleTimeoutImpl(fireTime);
			}

			/* (non-Javadoc)
			 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getTaskId()
			 */
			@Override
			public String getTaskId() {
				return prefix + vo.getXid();
			}
			
			
			@Override
			public String getThreadName() {
				return "Interval logging: " + vo.getXid();
			}
        	
        };
    }

    private void terminateIntervalLogging() {
        synchronized (intervalLoggingLock) {
        	//Always check because we may have been an interval logging point and we need to stop this.
            if(intervalLoggingTask != null) //Bug from UI where we are switching types of a running point
            	intervalLoggingTask.cancel();
        }
    }

    private void intervalSave(PointValueTime pvt) {
        synchronized (intervalLoggingLock) {
            if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.MAXIMUM) {
                if (intervalValue == null)
                    intervalValue = pvt;
                else if (pvt != null) {
                    if (intervalValue.getDoubleValue() < pvt.getDoubleValue())
                        intervalValue = pvt;
                }
            }
            else if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.MINIMUM) {
                if (intervalValue == null)
                    intervalValue = pvt;
                else if (pvt != null) {
                    if (intervalValue.getDoubleValue() > pvt.getDoubleValue())
                        intervalValue = pvt;
                }
            }
            else if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.AVERAGE){
                //Using the averaging values, ensure we keep the most recent values and pop off the old ones
            	if(vo.isOverrideIntervalLoggingSamples()){
	                while(averagingValues.size() >= vo.getIntervalLoggingSampleWindowSize()){ 
	                	averagingValues.remove(0); //Size -1 for the next item we are going to add
	                }
            	}
            	
            	averagingValues.add(pvt);
            }
        }
    }

    public void scheduleTimeoutImpl(long fireTime) {
        synchronized (intervalLoggingLock) {
            DataValue value;
            if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.INSTANT)
                value = PointValueTime.getValue(pointValue);
            else if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.MAXIMUM
                    || vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.MINIMUM) {
                value = PointValueTime.getValue(intervalValue);
                intervalValue = pointValue;
            }
            else if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.AVERAGE) {
            	
            	//We won't allow logging values until we have a full average window
            	//If we don't have enough averaging values then we will bail and wait for more
            	if(vo.isOverrideIntervalLoggingSamples() && (averagingValues.size() != vo.getIntervalLoggingSampleWindowSize()))
            		return;
            	
                IValueTime endValue = intervalValue;
                if (!averagingValues.isEmpty())
                    endValue = averagingValues.get(averagingValues.size() - 1);
                AnalogStatistics stats = new AnalogStatistics(intervalStartTime, fireTime, intervalValue,
                        averagingValues, endValue);
                if (stats.getAverage() == null)
                    value = null;
                else
                    value = new NumericValue(stats.getAverage());
                //Compute the center point of our average data, starting by finding where our period started
                long sampleWindowStartTime;
                if(vo.isOverrideIntervalLoggingSamples())
                	sampleWindowStartTime = averagingValues.get(0).getTime();
                else
                	sampleWindowStartTime = intervalStartTime; 
                
                intervalStartTime = fireTime;
                fireTime = sampleWindowStartTime + (fireTime - sampleWindowStartTime)/2L; //Fix to simulate center tapped filter (un-shift the average)
                intervalValue = pointValue;
                
                if(!vo.isOverrideIntervalLoggingSamples())
                	averagingValues.clear();
            }
            else
                throw new ShouldNeverHappenException("Unknown interval logging type: " + vo.getIntervalLoggingType());

            if (value != null){
            	PointValueTime newValue = new PointValueTime(value, fireTime);
                valueCache.logPointValueAsync(newValue, null);
                //Fire logged Events
                fireEvents(null, newValue, false, false, true, false);
            }
        }
    }
	
    //
    // / Purging
    //
    public void resetValues() {
        valueCache.reset();
        if (vo.getLoggingType() != DataPointVO.LoggingTypes.NONE)
            pointValue = valueCache.getLatestPointValue();
    }

    //
    // /
    // / Properties
    // /
    //
    public int getId() {
        return vo.getId();
    }

    @Override
    public PointValueTime getPointValue() {
        return pointValue;
    }

    @SuppressWarnings("unchecked")
    public <T extends PointLocatorRT<?>> T getPointLocator() {
        return (T) pointLocator;
    }

    public int getDataSourceId() {
        return vo.getDataSourceId();
    }

    public DataSourceVO<?> getDataSourceVO() {
        return dsVo;
    }
    
    public DataPointVO getVO() {
        return vo;
    }

    public List<PointEventDetectorRT<?>> getEventDetectors(){
    	return this.detectors;
    }
    
    @Override
    public int getDataTypeId() {
        return vo.getPointLocator().getDataTypeId();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + getId();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DataPointRT other = (DataPointRT) obj;
        if (getId() != other.getId())
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DataPointRT(id=" + getId() + ", name=" + vo.getName() + ")";
    }

    //
    // /
    // / Listeners
    // /
    //
    private void fireEvents(PointValueTime oldValue, PointValueTime newValue, boolean set, boolean backdate, boolean logged, boolean updated) {
        DataPointListener l = Common.runtimeManager.getDataPointListeners(vo.getId());
        if (l != null)
            Common.backgroundProcessing.addWorkItem(new EventNotifyWorkItem(vo.getXid(), l, oldValue, newValue, set, backdate, logged, updated));
    }

    class EventNotifyWorkItem implements WorkItem {
    	private final String prefix = "EN-";
    	private final String sourceXid;
        private final DataPointListener listener;
        private final PointValueTime oldValue;
        private final PointValueTime newValue;
        private final boolean set;
        private final boolean backdate;
        private final boolean logged;
        private final boolean updated;

        EventNotifyWorkItem(String xid, DataPointListener listener, PointValueTime oldValue, PointValueTime newValue, boolean set,
                boolean backdate, boolean logged, boolean updated) {
        	this.sourceXid = xid;
            this.listener = listener;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.set = set;
            this.backdate = backdate;
            this.logged = logged;
            this.updated = updated;
        }

        @Override
        public void execute() {
            if (backdate)
                listener.pointBackdated(newValue);
            else if(updated) {
                // Updated
        		listener.pointUpdated(newValue);

                // Fire if the point has changed.
                if (!PointValueTime.equalValues(oldValue, newValue))
                    listener.pointChanged(oldValue, newValue);

                // Fire if the point was set.
                if (set)
                    listener.pointSet(oldValue, newValue);
            }
            
            //Was this value actually logged
            if(logged)
            	listener.pointLogged(newValue);
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_MEDIUM;
        }

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Point event for: " + sourceXid + ", telling: " + listener.getListenerName();
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getTaskId()
		 */
		@Override
		public String getTaskId() {
			//So there is one task for each listener
			return prefix + sourceXid + "-" + listener.hashCode();
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getQueueSize()
		 */
		@Override
		public int getQueueSize() {
			return Task.UNLIMITED_QUEUE_SIZE;
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
		 */
		@Override
		public void rejected(RejectedTaskReason reason) {
			//No special handling, tracking/logging is handled by the WorkItemRunnable
		}

    }

    //
    //
    // Lifecycle
    //
    /*
     * For future use if we want to allow data points to startup
     *  in safe mode, will require changes to RuntimeManager
     * (non-Javadoc)
     * @see com.serotonin.util.ILifecycle#initialize(boolean)
     */
    public void initialize(boolean safe){
    	if(!safe)
    		initialize();
    }
    
    public void initialize() {
        // Get the latest value for the point from the database.
        pointValue = valueCache.getLatestPointValue();

        // Set the tolerance origin if this is a numeric
        if (pointValue != null && pointValue.getValue() instanceof NumericValue)
            toleranceOrigin = pointValue.getDoubleValue();

        // Add point event listeners
        for (AbstractPointEventDetectorVO<?> ped : vo.getEventDetectors()) {
            if (detectors == null)
                detectors = new ArrayList<PointEventDetectorRT<?>>();

            PointEventDetectorRT<?> pedRT = (PointEventDetectorRT<?>) ped.createRuntime();
            detectors.add(pedRT);
            pedRT.initialize();
            Common.runtimeManager.addDataPointListener(vo.getId(), pedRT);
        }

        //initializeIntervalLogging();
    }

    @Override
    public void terminate() {
        terminateIntervalLogging();

        if (detectors != null) {
            for (PointEventDetectorRT<?> pedRT : detectors) {
                Common.runtimeManager.removeDataPointListener(vo.getId(), pedRT);
                pedRT.terminate();
            }
        }
        Common.eventManager.cancelEventsForDataPoint(vo.getId());
    }

    @Override
    public void joinTermination() {
        // no op
    }

    public void initializeHistorical() {
        initializeIntervalLogging(0l, false);
    }

    public void terminateHistorical() {
        terminateIntervalLogging();
    }


    /**
     * Update the value in the cache with the option to log to DB.
     * 
     * This only updates an existing value
     * 
     * Caution, this bypasses the Logging Settings
     * 
     * @param newValue
     * @param source
     * @param logValue
     * @param async
     */
	public void updatePointValueInCache(PointValueTime newValue, SetPointSource source, boolean logValue, boolean async) {
        valueCache.updatePointValue(newValue, source, logValue, async);
	}
	
	/**
	 * Get a copy of the current cache
	 * @return
	 */
	public List<PointValueTime> getCacheCopy(){
		List<PointValueTime> copy = new ArrayList<PointValueTime>(this.valueCache.getCacheContents().size());
		for(PointValueTime pvt : this.valueCache.getCacheContents())
			copy.add(pvt);
		return copy;
	}

	@Override
	public DataPointWrapper getDataPointWrapper(AbstractPointWrapper rtWrapper) {
		return new DataPointWrapper(vo, rtWrapper);
	}

}
