/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.statistics.AnalogStatistics;
import com.infiniteautomation.mango.statistics.StartsAndRuntime;
import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.EnhancedPointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.rt.script.AbstractPointWrapper;
import com.serotonin.m2m2.rt.script.DataPointWrapper;
import com.serotonin.m2m2.util.ExceptionListWrapper;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycle;

public class DataPointRT implements IDataPointValueSource, ILifecycle {
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
    private AbstractTimer timer;
    
    /**
     * This is the value around which tolerance decisions will be made when determining whether to log numeric values.
     */
    private double toleranceOrigin;

    /**
     * @param vo
     * @param pointLocator
     * @param dsVo
     * @param initialCache
     */
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
	            //Must be after 'time' and before 'after'
	            if (pvt.getTime() < after.getTime() && pvt.getTime() > time)
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
        //TODO This can expand the cache, perhaps we want to not expand the cache and then fill to limit
        // from the db.
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

        if (newValue.getTime() > Common.timer.currentTimeMillis() + SystemSettingsDao.instance.getFutureDateLimit()) {
            // Too far future dated. Toss it. But log a message first.
            LOG.warn("Discarding point value", new Exception("Future dated value detected: pointId=" + vo.getId() + ", value=" + newValue.getValue().toString()
                    + ", type=" + vo.getPointLocator().getDataTypeId() + ", ts=" + newValue.getTime()));
            return;
        }

        boolean backdated = pointValue != null && newValue.getTime() < pointValue.getTime();

        // Determine whether the new value qualifies for logging.
        boolean logValue;
        // ... or even saving in the cache.
        boolean saveValue = true;
        switch (vo.getLoggingType()) {
        case DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL:
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
                    logValue = !Objects.equals(newValue.getValue(), pointValue.getValue());
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
            saveValue = true; //We want to save the intra-interval values in the cache
        default:
            logValue = false;
        }
        
        if(!saveToDatabase)
        	logValue = false;
        
        if (saveValue) {
            valueCache.savePointValue(newValue, source, logValue, async);
            if(vo.getLoggingType() == DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL)
                rescheduleChangeInterval(Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod()));
        }

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
            fireEvents(oldValue, newValue, null, source != null, false, logValue, true, false);
        }
        else
            fireEvents(null, newValue, null, false, true, logValue, false, false);
    }

    public static enum FireEvents {
        ALWAYS,
        ON_CURRENT_VALUE_UPDATE,
        NEVER;
    }
    
    /**
     * 
     * This method is called by modules that have the potential to generate a rapid flow of values and backdates
     *  for the purpose of circumventing the update method's various controls on logging behaviors. It does not
     *  generate events because the expected rate of calls from the modules which use it is somewhere between high
     *  and very high.
     * 
     * @param newValue - the new value
     * @param source - for annotation
     * @param logValue - should the value be logged?
     * @param async - should this be done asynchronously i.e. queued in a batch 
     */
    public void savePointValueDirectToCache(PointValueTime newValue, SetPointSource source, boolean logValue,
            boolean async) {
        savePointValueDirectToCache(newValue, source, logValue, async, FireEvents.NEVER);
    }
    
    /**
     * This method is called by modules that have the potential to generate a rapid flow of values and backdates
     *  for the purpose of circumventing the update method's various controls on logging behaviors. It can generate events
     *  if desired.
     * 
     * @param newValue - the new value
     * @param source - for annotation
     * @param logValue - should the value be logged?
     * @param async - should this be done asynchronously i.e. queued in a batch 
     * @param fireEvents - how to fire events, 0=never, 1=if new value's ts is >= pointValue's ts, 2=always
     */
    public void savePointValueDirectToCache(PointValueTime newValue, SetPointSource source, boolean logValue,
            boolean async, FireEvents fireEvents) {
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

        if (newValue.getTime() > Common.timer.currentTimeMillis() + SystemSettingsDao.instance.getFutureDateLimit()) {
            // Too far future dated. Toss it. But log a message first.
            LOG.warn("Discarding point value", new Exception("Future dated value detected: pointId=" + vo.getId() + ", value=" + newValue.getValue().toString()
                    + ", type=" + vo.getPointLocator().getDataTypeId() + ", ts=" + newValue.getTime()));
            return;
        }
        
        // add annotation to newValue before firing events so event detectors can
        // fetch the annotation
        if (source != null) {
            newValue = new AnnotatedPointValueTime(newValue.getValue(),
                    newValue.getTime(), source.getSetPointSourceMessage());
        }

        valueCache.savePointValue(newValue, source, logValue, async);
        
        //Update our value if it is newer
        if (pointValue == null || newValue.getTime() >= pointValue.getTime()) {
            PointValueTime oldValue = pointValue;
            pointValue = newValue;
            if(fireEvents == FireEvents.ON_CURRENT_VALUE_UPDATE || fireEvents == FireEvents.ALWAYS)
                fireEvents(oldValue, newValue, null, source != null, false, logValue, true, false);
        }else if(fireEvents == FireEvents.ALWAYS)
            fireEvents(pointValue, newValue, null, source != null, false, logValue, false, false);

    }

    //
    // / Interval logging
    //
    /**
     * 
     */
    public void initializeIntervalLogging(long nextPollTime, boolean quantize) {
        if(vo.getLoggingType() != DataPointVO.LoggingTypes.INTERVAL && vo.getLoggingType() != DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL)
            return;
        
        synchronized (intervalLoggingLock) {
            
            long loggingPeriodMillis = Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod());
            long delay = loggingPeriodMillis;
            if(quantize){
                    // Quantize the start.
                    //Compute delay only if we are offset from the next poll time
                    long nextPollOffset = (nextPollTime % loggingPeriodMillis);
                    if(nextPollOffset != 0)
                        delay = loggingPeriodMillis - nextPollOffset;
                    LOG.debug("First interval log should be at: " + (nextPollTime + delay));
            }
            Date startTime = new Date(nextPollTime + delay);
            
            if (vo.getLoggingType() == DataPointVO.LoggingTypes.INTERVAL) {
                intervalValue = pointValue;
                if (vo.getIntervalLoggingType() == DataPointVO.IntervalLoggingTypes.AVERAGE) {
                    intervalStartTime = timer == null ? Common.timer.currentTimeMillis() : timer.currentTimeMillis();
                    if(averagingValues.size() > 0) {
                            AnalogStatistics stats = new AnalogStatistics(intervalStartTime-loggingPeriodMillis, intervalStartTime, null, averagingValues);
                            PointValueTime newValue = new PointValueTime(stats.getAverage(), intervalStartTime);
                        valueCache.savePointValue(newValue, null, true, true);
                        //Fire logged Events
                        fireEvents(null, newValue, null, false, false, true, false, false);
                            averagingValues.clear();
                    }
                }
                //Are we using a custom timer?
                if(this.timer == null)
    	            intervalLoggingTask = new TimeoutTask(new FixedRateTrigger(startTime, loggingPeriodMillis), createIntervalLoggingTimeoutClient());
                else
    	            intervalLoggingTask = new TimeoutTask(new FixedRateTrigger(startTime, loggingPeriodMillis), createIntervalLoggingTimeoutClient(), this.timer);
            } else if(vo.getLoggingType() == DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL) {
                if(this.timer == null)
                    intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(startTime), createIntervalLoggingTimeoutClient());
                else
                    intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(startTime), createIntervalLoggingTimeoutClient(), timer);
            }
        }
    }
    
    private void rescheduleChangeInterval(long delay) {
        synchronized(intervalLoggingLock) {
            if(intervalLoggingTask != null)
                intervalLoggingTask.cancel();
            
            if(intervalStartTime != Long.MIN_VALUE) {
                if(this.timer == null)
                    intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(delay), createIntervalLoggingTimeoutClient());
                else
                    intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(delay), createIntervalLoggingTimeoutClient(), timer);
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
            intervalStartTime = Long.MIN_VALUE; //Signal to cancel ON_CHANGE_INTERVAL rescheduling
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
            if(vo.getLoggingType() == DataPointVO.LoggingTypes.INTERVAL) {
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
                    
                    if(vo.getPointLocator().getDataTypeId() == DataTypes.MULTISTATE) {
                        StartsAndRuntimeList stats = new StartsAndRuntimeList(intervalStartTime, fireTime, intervalValue, averagingValues);
                        double maxProportion = -1;
                        Object valueAtMax = null;
                        for(StartsAndRuntime sar : stats.getData()) {
                            if(sar.getProportion() > maxProportion) {
                                maxProportion = sar.getProportion();
                                valueAtMax = sar.getValue();
                            }
                        }
                        if(valueAtMax != null)
                            value = new MultistateValue(DataValue.objectToValue(valueAtMax).getIntegerValue());
                        else
                            value = null;
                    } else {
                        AnalogStatistics stats = new AnalogStatistics(intervalStartTime, fireTime, intervalValue, averagingValues);
                        if (stats.getAverage() == null || (stats.getAverage() == Double.NaN && stats.getCount() == 0))
                            value = null;
                        else if(vo.getPointLocator().getDataTypeId() == DataTypes.NUMERIC)
                            value = new NumericValue(stats.getAverage());
                        else if(vo.getPointLocator().getDataTypeId() == DataTypes.BINARY)
                            value = new BinaryValue(stats.getAverage() >= 0.5);
                        else
                            throw new ShouldNeverHappenException("Unsupported average interval logging data type.");
                    }
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
            } else if(vo.getLoggingType() == DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL) {
                //Okay, no changes rescheduled the timer. Get a value,
                if(pointValue != null) {
                    value = pointValue.getValue();
                    if(vo.getPointLocator().getDataTypeId() == DataTypes.NUMERIC)
                        toleranceOrigin = pointValue.getDoubleValue();
                } else
                    value = null;

                if(this.timer == null) // ...and reschedule
                    intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod())), createIntervalLoggingTimeoutClient());
                else
                    intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod())), createIntervalLoggingTimeoutClient(), timer);
            } else
                value = null;

            if (value != null){
            	PointValueTime newValue = new PointValueTime(value, fireTime);
                valueCache.savePointValue(newValue, null, true, true);
                //Fire logged Events
                fireEvents(null, newValue, null, false, false, true, false, false);
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
    
    public void resetValues(long before) {
        valueCache.reset(before);
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
    
    @Override
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
        Object previous = attributes.put(key, value);
        if(previous == null || !previous.equals(value)) {
            Map<String, Object> attributesCopy = new HashMap<>(attributes);
            fireEvents(null, null, attributesCopy, false, false, false, false, true);
        }
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
    protected void fireEvents(PointValueTime oldValue, PointValueTime newValue, Map<String, Object> attributes, boolean set, 
            boolean backdate, boolean logged, boolean updated, boolean attributesChanged) {
        DataPointListener l = Common.runtimeManager.getDataPointListeners(vo.getId());
        if (l != null)
            Common.backgroundProcessing.addWorkItem(new EventNotifyWorkItem(vo.getXid(), l, oldValue, newValue, 
                    attributes, set, backdate, logged, updated, attributesChanged));
    }

    class EventNotifyWorkItem implements WorkItem {
        private static final String descriptionPrefix = "Point event for: ";
    	private static final String prefix = "EN-";
    	private final String sourceXid;
        private final DataPointListener listener;
        private final PointValueTime oldValue;
        private final PointValueTime newValue;
        private final Map<String, Object> attributes;
        private final boolean set;
        private final boolean backdate;
        private final boolean logged;
        private final boolean updated;
        private final boolean attributesChanged;

        EventNotifyWorkItem(String xid, DataPointListener listener, PointValueTime oldValue, PointValueTime newValue, Map<String, Object> attributes, boolean set,
                boolean backdate, boolean logged, boolean updated, boolean attributesChanged) {
            this.sourceXid = xid;
            this.listener = listener;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.attributes = attributes;
            this.set = set;
            this.backdate = backdate;
            this.logged = logged;
            this.updated = updated;
            this.attributesChanged = attributesChanged;
        }

        @Override
        public void execute() {
            try {
                if (attributesChanged) {
                    listener.attributeChanged(attributes);
                    return;
                }
    
                if (backdate)
                    listener.pointBackdated(newValue);
                else if (updated) {
                    // Updated
                    listener.pointUpdated(newValue);
    
                    // Fire if the point has changed.
                    if (!PointValueTime.equalValues(oldValue, newValue))
                        listener.pointChanged(oldValue, newValue);
    
                    // Fire if the point was set.
                    if (set)
                        listener.pointSet(oldValue, newValue);
                }
    
                // Was this value actually logged
                if (logged)
                    listener.pointLogged(newValue);
            } catch(ExceptionListWrapper e) {
                LOG.warn("Exceptions in event notify work item.");
                for(Exception e2 : e.getExceptions())
                    LOG.warn("Listener exception: " + e2.getMessage(), e2);
            }
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
			return descriptionPrefix + sourceXid;
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
        if(timer != null) {
            pointValue = getPointValueBefore(timer.currentTimeMillis());
            initializeIntervalLogging(timer.currentTimeMillis(), false);
        } else
            initializeIntervalLogging(0l, false);
    }

    public void terminateHistorical() {
        terminateIntervalLogging();
        pointValue = valueCache.getLatestPointValue();
    }
	
	/**
	 * Get a copy of the current cache
	 * @return
	 */
	public List<PointValueTime> getCacheCopy(){
	    List<PointValueTime> c = this.valueCache.getCacheContents();
		return new ArrayList<PointValueTime>(c);
	}
	
	/**
     * Get a copy of the current cache, size limited
     * @return
     */
    public List<PointValueTime> getCacheCopy(int limit){
        List<PointValueTime> copy = new ArrayList<PointValueTime>(this.valueCache.getCacheContents().size());
        int size = 0;
        for(PointValueTime pvt : this.valueCache.getCacheContents()) {
            copy.add(pvt);
            size++;
            if(size == limit)
                break;
        }
        return copy;
    }

	@Override
	public DataPointWrapper getDataPointWrapper(AbstractPointWrapper rtWrapper) {
		return new DataPointWrapper(vo, rtWrapper);
	}

}
