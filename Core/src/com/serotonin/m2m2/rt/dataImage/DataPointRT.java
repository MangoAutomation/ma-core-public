/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

import com.infiniteautomation.mango.pointvaluecache.PointValueCache;
import com.infiniteautomation.mango.spring.esb.PointValueTimeTopic;
import com.infiniteautomation.mango.statistics.AnalogStatistics;
import com.infiniteautomation.mango.statistics.StartsAndRuntime;
import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.infiniteautomation.mango.util.LazyField;
import com.radixiot.pi.grpc.MangoPointValueTime;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.DataPointEventNotifyWorkItem;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.dataSource.PointLocatorRT;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.rt.script.AbstractPointWrapper;
import com.serotonin.m2m2.rt.script.DataPointWrapper;
import com.serotonin.m2m2.util.ExceptionListWrapper;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.DataPointVO.IntervalLoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.LoggingTypes;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycle;
import com.serotonin.util.ILifecycleState;

public class DataPointRT implements IDataPointValueSource, ILifecycle {
    private final Logger log = LoggerFactory.getLogger(DataPointRT.class);
    private static final PvtTimeComparator pvtTimeComparator = new PvtTimeComparator();
    private static final String prefix = "INTVL_LOG-";

    // Configuration data.
    private final DataPointVO vo;
    private final DataSourceRT<? extends DataSourceVO> dataSource;
    private final PointLocatorRT<?> pointLocator;

    // Runtime data.
    private final LazyField<PointValueTime> pointValue;
    private final DataPointRTPointValueCache valueCache;
    private List<PointEventDetectorRT<?>> detectors;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    // Interval logging data.
    private PointValueTime intervalValue;
    private long intervalStartTime = -1;
    private List<IValueTime<DataValue>> averagingValues;
    private final Object intervalLoggingLock = new Object();
    private volatile TimerTask intervalLoggingTask;

    //Simulation Timer, or any timer implementation
    private AbstractTimer timer;

    private volatile ILifecycleState state = ILifecycleState.PRE_INITIALIZE;

    /**
     * This is the value around which tolerance decisions will be made when determining whether to log numeric values.
     */
    private double toleranceOrigin;

    private final DataPointDao dataPointDao;

    public DataPointRT(DataPointWithEventDetectors dp, PointLocatorRT<?> pointLocator, DataSourceRT<? extends DataSourceVO> dataSource, List<PointValueTime> initialCache, PointValueDao dao, PointValueCache pointValueCache) {
        if (dataSource.getId() != dp.getDataPoint().getDataSourceId()) {
            throw new IllegalStateException("Wrong data source for provided point");
        }

        this.vo = dp.getDataPoint();
        this.detectors = new ArrayList<>();
        for (AbstractPointEventDetectorVO ped : dp.getEventDetectors()) {
            PointEventDetectorRT<?> pedRT = (PointEventDetectorRT<?>) ped.createRuntime();
            detectors.add(pedRT);
        }
        this.dataSource = dataSource;
        this.pointLocator = pointLocator;
        this.valueCache = new DataPointRTPointValueCache(vo, vo.getDefaultCacheSize(), initialCache, dao, pointValueCache);

        if(vo.getIntervalLoggingType() == IntervalLoggingTypes.AVERAGE) {
            averagingValues = new ArrayList<IValueTime<DataValue>>();
        }
        this.pointValue = new LazyField<>(() -> {
            PointValueTime pvt = valueCache.getLatestPointValue();
            // Set the tolerance origin if this is a numeric
            if (pvt != null && pvt.getValue() instanceof NumericValue)
                toleranceOrigin = pvt.getDoubleValue();
            return pvt;
        });

        this.dataPointDao = Common.getBean(DataPointDao.class);
    }

    /**
     * To allow simulation of points using a timer implementation
     *
     */
    public DataPointRT(DataPointWithEventDetectors vo, PointLocatorRT<?> pointLocator, DataSourceRT<? extends DataSourceVO> dataSource,
                       List<PointValueTime> initialCache, PointValueDao dao, PointValueCache pointValueCache, AbstractTimer timer) {
        this(vo, pointLocator, dataSource, initialCache, dao, pointValueCache);
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

        return Common.getBean(PointValueDao.class).getPointValueBefore(vo, time).orElse(null);
    }

    @Override
    public PointValueTime getPointValueAt(long time) {
        for (PointValueTime pvt : valueCache.getCacheContents()) {
            if (pvt.getTime() == time)
                return pvt;
        }

        return Common.getBean(PointValueDao.class).getPointValueAt(vo, time).orElse(null);
    }

    @Override
    public PointValueTime getPointValueAfter(long time) {

        //Get the value stored in the db
        PointValueTime after = Common.getBean(PointValueDao.class).getPointValueAfter(vo, time).orElse(null);

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
        return valueCache.getLatestPointValues(limit);
    }

    public List<PointValueTime> getLatestPointValues() {
        return valueCache.getCacheContents();
    }

    @Override
    public List<PointValueTime> getPointValues(long since) {
        List<PointValueTime> result = Common.getBean(PointValueDao.class).getPointValues(vo, since);

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
        List<PointValueTime> result = Common.getBean(PointValueDao.class).getPointValuesBetween(vo, from, to);

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
        if (newValue == null || newValue.getValue() == null)
            return;

        // Check if this value qualifies for discardation.
        if(discardUnwantedValues(newValue)) {
            return;
        }

        PointValueTime oldValue = pointValue.get();
        boolean backdated = oldValue != null && newValue.getTime() < oldValue.getTime();

        // Determine whether the new value qualifies for logging.
        boolean logValue;
        // ... or even saving in the cache.
        boolean saveValue = true;
        switch (vo.getLoggingType()) {
            case LoggingTypes.ON_CHANGE_INTERVAL:
            case LoggingTypes.ON_CHANGE:
                if (oldValue == null) {
                    logValue = true;
                    if(newValue.getValue() instanceof NumericValue) {
                        //Set the tolerance origin so the next value has
                        // something to compare to
                        toleranceOrigin = newValue.getDoubleValue();
                    }
                }else if (backdated)
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

                        if (diff > vo.getTolerance() || Double.isNaN(newd) != Double.isNaN(toleranceOrigin)) {
                            toleranceOrigin = newd;
                            logValue = true;
                        }
                        else
                            logValue = false;
                    } else {
                        logValue = !Objects.equals(newValue.getValue(), oldValue.getValue());
                    }
                }

                saveValue = logValue;
                break;
            case LoggingTypes.ALL:
                logValue = true;
                break;
            case LoggingTypes.ON_TS_CHANGE:
                if (oldValue == null)
                    logValue = true;
                else if (backdated)
                    // Backdated. Ignore it
                    logValue = false;
                else
                    logValue = newValue.getTime() != oldValue.getTime();

                saveValue = logValue;
                break;
            case LoggingTypes.INTERVAL:
                if (!backdated)
                    intervalSave(newValue);
            default:
                logValue = false;
        }

        if(!saveToDatabase)
            logValue = false;

        if (saveValue) {
            valueCache.savePointValue(newValue, source, logValue, async);
            if(vo.getLoggingType() == LoggingTypes.ON_CHANGE_INTERVAL)
                rescheduleChangeInterval(Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod()));
        }

        // add annotation to newValue before firing events so event detectors can
        // fetch the annotation
        if (source != null && source.getSetPointSourceMessage() != null) {
            newValue = new AnnotatedPointValueTime(newValue.getValue(),
                    newValue.getTime(), source.getSetPointSourceMessage());
        }

        if(!backdated) {
            pointValue.set(newValue);
        }

        //fireEvents(oldValue, newValue, null, source != null, backdated, logValue, !backdated, false);
        KafkaTemplate<String, MangoPointValueTime> template = Common.getBean(KafkaTemplate.class, "protoKafkaTemplate");
        MangoPointValueTime msg = MangoPointValueTime.newBuilder()
                .setDataPointId(getId())
                .setTimestamp(newValue.getTime())
                .setValue(newValue.getDoubleValue()).build();

        //use send of type message to leverage spring messaging protobuf converter
        template.send(new Message<MangoPointValueTime>() {

            @Override
            public MangoPointValueTime getPayload() {
                return msg;
            }

            @Override
            public MessageHeaders getHeaders() {
                Map<String, Object> headers = new HashMap<>();
                headers.put(MessageHeaders.CONTENT_TYPE, new MimeType("application", "x-protobuf", StandardCharsets.UTF_8).toString());
                headers.put(KafkaHeaders.TOPIC, PointValueTimeTopic.TOPIC);
                return new MessageHeaders(headers);
            }
        });

    }

    public static enum FireEvents {
        ALWAYS,
        ON_CURRENT_VALUE_UPDATE,
        NEVER;
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
        if (newValue == null || newValue.getValue() == null)
            return;

        // Check if this value qualifies for discardation.
        if(discardUnwantedValues(newValue)) {
            return;
        }

        if (newValue.getTime() > Common.timer.currentTimeMillis() + SystemSettingsDao.getInstance().getFutureDateLimit()) {
            // Too far future dated. Toss it. But log a message first.
            log.warn("Discarding point value", new Exception("Future dated value detected: pointId=" + vo.getId() + ", value=" + newValue.getValue().toString()
                    + ", type=" + vo.getPointLocator().getDataType() + ", ts=" + newValue.getTime()));
            return;
        }

        // add annotation to newValue before firing events so event detectors can
        // fetch the annotation
        if (source != null) {
            newValue = newValue.withAnnotationFromSource(source);
        }

        valueCache.savePointValue(newValue, source, logValue, async);

        //Update our value if it is newer
        if (pointValue.get() == null || newValue.getTime() >= pointValue.get().getTime()) {
            PointValueTime oldValue = pointValue.get();
            pointValue.set(newValue);
            if(fireEvents == FireEvents.ON_CURRENT_VALUE_UPDATE || fireEvents == FireEvents.ALWAYS)
                fireEvents(oldValue, newValue, null, source != null, false, logValue, true, false);
        }else if(fireEvents == FireEvents.ALWAYS)
            fireEvents(pointValue.get(), newValue, null, source != null, true, logValue, false, false);

    }

    /**
     * Should this value be discarded? The rules are:
     *  - mismatched value data type to point data type
     *  - if discarding extreme values then discard NaN or outside of extreme limits
     *  - anything too far in the future, see future date limit system setting
     *
     */
    private boolean discardUnwantedValues(PointValueTime pvt) {

        // Check the data type of the value against that of the locator, just for fun.
        DataType valueDataType = pvt.getValue().getDataType();
        if (valueDataType != vo.getPointLocator().getDataType())
            // This should never happen, but if it does it can have serious downstream consequences. Also, we need
            // to know how it happened, and the stack trace here provides the best information.
            throw new ShouldNeverHappenException("Data type mismatch between new value and point locator: newValue="
                    + pvt.getValue().getDataType() + ", locator=" + vo.getPointLocator().getDataType());

        if (vo.isDiscardExtremeValues() && vo.getPointLocator().getDataType()== DataType.NUMERIC) {
            double newd = pvt.getDoubleValue();
            //Discard if NaN
            if(Double.isNaN(newd))
                return true;

            if (newd < vo.getDiscardLowLimit() || newd > vo.getDiscardHighLimit()) {
                return true;
            }else {
                return false;
            }
        }

        //Future date check
        if (pvt.getTime() > Common.timer.currentTimeMillis() + SystemSettingsDao.getInstance().getFutureDateLimit()) {
            // Too far future dated. Toss it. But log a message first.
            log.warn("Discarding point value", new Exception("Future dated value detected: pointId="
                    + vo.getId() + ", value=" + pvt.getValue().toString()
                    + ", type=" + vo.getPointLocator().getDataType() + ", ts=" + pvt.getTime()));
            return true;
        }
        return false;
    }

    //
    // / Interval logging
    //

    public boolean isIntervalLogging() {
        return vo.getLoggingType() == LoggingTypes.INTERVAL ||
                vo.getLoggingType() == LoggingTypes.ON_CHANGE_INTERVAL;
    }

    public void initializeIntervalLogging(long nextPollTime, boolean quantize) {
        if (!isIntervalLogging() || intervalLoggingTask != null) return;

        // double checked lock
        synchronized (intervalLoggingLock) {
            // polling data sources call initializeIntervalLogging() when point is added to poll
            // however some hybrid data sources such as BACnetDataSourceRT may call initializeIntervalLogging()
            // earlier in response to an event.
            if (intervalLoggingTask != null) return;

            long loggingPeriodMillis = Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod());
            long delay = loggingPeriodMillis;
            if(quantize){
                // Quantize the start.
                //Compute delay only if we are offset from the next poll time
                long nextPollOffset = (nextPollTime % loggingPeriodMillis);
                if(nextPollOffset != 0)
                    delay = loggingPeriodMillis - nextPollOffset;

                if (log.isDebugEnabled()) {
                    log.debug(String.format("First interval log should be at: %s (%d)", new Date(nextPollTime + delay), nextPollTime + delay));
                }
            }
            Date startTime = new Date(nextPollTime + delay);

            if (vo.getLoggingType() == LoggingTypes.INTERVAL) {
                intervalValue = pointValue.get();
                if (vo.getIntervalLoggingType() == IntervalLoggingTypes.AVERAGE) {
                    intervalStartTime = timer == null ? Common.timer.currentTimeMillis() : timer.currentTimeMillis();
                    if(averagingValues.size() > 0) {
                        AnalogStatistics stats = new AnalogStatistics(intervalStartTime-loggingPeriodMillis, intervalStartTime, null, averagingValues);
                        PointValueTime newValue = new PointValueTime(stats.getAverage(), intervalStartTime);
                        // Save the new value and get a point value time back that has the id and annotations set, as appropriate.
                        valueCache.savePointValueAsync(newValue);
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
            } else if(vo.getLoggingType() == LoggingTypes.ON_CHANGE_INTERVAL) {
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
            if (vo.getIntervalLoggingType() == IntervalLoggingTypes.MAXIMUM) {
                if (intervalValue == null)
                    intervalValue = pvt;
                else if (pvt != null) {
                    if (intervalValue.getDoubleValue() < pvt.getDoubleValue())
                        intervalValue = pvt;
                }
            }
            else if (vo.getIntervalLoggingType() == IntervalLoggingTypes.MINIMUM) {
                if (intervalValue == null)
                    intervalValue = pvt;
                else if (pvt != null) {
                    if (intervalValue.getDoubleValue() > pvt.getDoubleValue())
                        intervalValue = pvt;
                }
            }
            else if (vo.getIntervalLoggingType() == IntervalLoggingTypes.AVERAGE){
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
            if(vo.getLoggingType() == LoggingTypes.INTERVAL) {
                if (vo.getIntervalLoggingType() == IntervalLoggingTypes.INSTANT)
                    value = PointValueTime.getValue(pointValue.get());
                else if (vo.getIntervalLoggingType() == IntervalLoggingTypes.MAXIMUM
                        || vo.getIntervalLoggingType() == IntervalLoggingTypes.MINIMUM) {
                    value = PointValueTime.getValue(intervalValue);
                    intervalValue = pointValue.get();
                }
                else if (vo.getIntervalLoggingType() == IntervalLoggingTypes.AVERAGE) {

                    //We won't allow logging values until we have a full average window
                    //If we don't have enough averaging values then we will bail and wait for more
                    if(vo.isOverrideIntervalLoggingSamples() && (averagingValues.size() != vo.getIntervalLoggingSampleWindowSize()))
                        return;

                    if(vo.getPointLocator().getDataType() == DataType.MULTISTATE) {
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
                        else if(vo.getPointLocator().getDataType() == DataType.NUMERIC)
                            value = new NumericValue(stats.getAverage());
                        else if(vo.getPointLocator().getDataType() == DataType.BINARY)
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
                    intervalValue = pointValue.get();

                    if(!vo.isOverrideIntervalLoggingSamples())
                        averagingValues.clear();
                }
                else
                    throw new ShouldNeverHappenException("Unknown interval logging type: " + vo.getIntervalLoggingType());
            } else if(vo.getLoggingType() == LoggingTypes.ON_CHANGE_INTERVAL) {
                //Okay, no changes rescheduled the timer. Get a value,
                if(pointValue.get() != null) {
                    value = pointValue.get().getValue();
                    if(vo.getPointLocator().getDataType() == DataType.NUMERIC)
                        toleranceOrigin = pointValue.get().getDoubleValue();
                } else
                    value = null;

                if(intervalStartTime != Long.MIN_VALUE) {
                    if(this.timer == null) // ...and reschedule
                        intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod())), createIntervalLoggingTimeoutClient());
                    else
                        intervalLoggingTask = new TimeoutTask(new OneTimeTrigger(Common.getMillis(vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod())), createIntervalLoggingTimeoutClient(), timer);
                }
            } else
                value = null;

            if (value != null) {
                PointValueTime newValue = new PointValueTime(value, fireTime);
                // Check if this value qualifies for discardation.
                if(discardUnwantedValues(newValue)) {
                    return;
                }

                // Save the new value and get a point value time back that has the id and annotations set, as appropriate.
                valueCache.savePointValueAsync(newValue);
                //Fire logged Events
                fireEvents(null, newValue, null, false, false, true, false, false);
            }
        }
    }

    //
    // / Purging
    //
    public void invalidateCache() {
        invalidateCache(true);
    }

    public void invalidateCache(boolean invalidatePersisted) {
        valueCache.invalidate(invalidatePersisted);
        if (vo.getLoggingType() != LoggingTypes.NONE) {
            pointValue.set(valueCache.getLatestPointValue());
        }
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
        return pointValue.get();
    }

    @SuppressWarnings("unchecked")
    public <T extends PointLocatorRT<?>> T getPointLocator() {
        return (T) pointLocator;
    }

    public int getDataSourceId() {
        return dataSource.getId();
    }

    public DataSourceRT<? extends DataSourceVO> getDataSource() {
        return dataSource;
    }

    public DataSourceVO getDataSourceVO() {
        return dataSource.getVo();
    }

    @Override
    public DataPointVO getVO() {
        return vo;
    }

    public List<PointEventDetectorRT<?>> getEventDetectors(){
        return this.detectors;
    }

    @Override
    public DataType getDataType() {
        return vo.getPointLocator().getDataType();
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
            Common.backgroundProcessing.addWorkItem(new DataPointEventNotifyWorkItem(vo.getXid(), l, oldValue, newValue,
                    attributes, set, backdate, logged, updated, attributesChanged));
    }

    @Override
    public ILifecycleState getLifecycleState() {
        return state;
    }

    //
    //
    // Lifecycle
    //
    /*
     * For future use if we want to allow data points to startup
     *  in safe mode, will require changes to RuntimeManager
     *
     */
    @Override
    public final synchronized void initialize(boolean safe) {
        ensureState(ILifecycleState.PRE_INITIALIZE);
        this.state = ILifecycleState.INITIALIZING;
        notifyStateChanged();

        try {
            initialize();
            initializeDetectors();
            // If we are a polling data source then we need to wait to start our interval logging until the first poll due to quantization
            if (dataSource.shouldInitializeIntervalLogging(this)) {
                initializeIntervalLogging(0L, false);
            }
            initializeListeners();

            // add ourselves to the data source for the next poll
            dataSource.addDataPoint(this);
        } catch (Exception e) {
            try {
                terminate();
                joinTermination();
            }catch(Exception e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }

        this.state = ILifecycleState.RUNNING;
        notifyStateChanged();
    }

    private void initializeDetectors() {
        for (PointEventDetectorRT<?> pedRT : detectors) {
            pedRT.initialize();
            Common.runtimeManager.addDataPointListener(vo.getId(), pedRT);
        }
    }

    private void initializeListeners() {
        DataPointListener l = Common.runtimeManager.getDataPointListeners(getId());
        if (l != null) {
            try {
                l.pointInitialized();
            } catch (ExceptionListWrapper e) {
                log.warn("Exceptions in point initialized listeners' methods.");
                for (Exception e2 : e.getExceptions())
                    log.warn("Listener exception: " + e2.getMessage(), e2);
            } catch (Exception e) {
                log.warn("Exception in point initialized listener's method: " + e.getMessage(), e);
            }
        }
    }

    protected void initialize() {
    }

    @Override
    public final synchronized void terminate() {
        ensureState(ILifecycleState.INITIALIZING, ILifecycleState.RUNNING);
        this.state = ILifecycleState.TERMINATING;
        notifyStateChanged();

        boolean dataSourceTerminating = dataSource.getLifecycleState() == ILifecycleState.TERMINATING;
        if (!dataSourceTerminating) {
            // Data source clears all its points at once when it is terminating
            dataSource.removeDataPoint(this);
        }

        try {
            terminateListeners();
        } catch (Exception e) {
            log.error("Failed to terminate listeners for " + readableIdentifier(), e);
        }

        try {
            terminateIntervalLogging();
        } catch (Exception e) {
            log.error("Failed to terminate interval logging for " + readableIdentifier(), e);
        }

        try {
            terminateDetectors();
        } catch (Exception e) {
            log.error("Failed to terminate detectors for " + readableIdentifier(), e);
        }

        try {
            if (!dataSourceTerminating) {
                // Data source cancels all its point events at once when it is terminating
                Common.eventManager.cancelEventsForDataPoint(getId());
            }
        } catch (Exception e) {
            log.error("Failed to cancel events for " + readableIdentifier(), e);
        }

        this.state = ILifecycleState.TERMINATED;
        notifyStateChanged();
        Common.runtimeManager.removeDataPoint(this);
    }

    private void terminateDetectors() {
        if (detectors != null) {
            for (PointEventDetectorRT<?> pedRT : detectors) {
                Common.runtimeManager.removeDataPointListener(vo.getId(), pedRT);
                pedRT.terminate();
            }
        }
    }

    private void terminateListeners() {
        DataPointListener l = Common.runtimeManager.getDataPointListeners(getId());
        if (l != null) {
            try {
                l.pointTerminated(getVO());
            } catch (ExceptionListWrapper e) {
                log.warn("Exceptions in point terminated method.");
                for (Exception e2 : e.getExceptions())
                    log.warn("Listener exception: " + e2.getMessage(), e2);
            }
        }
    }

    @Override
    public final synchronized void joinTermination() {
        // no op
    }

    public void initializeHistorical() {
        if(timer != null) {
            pointValue.set(getPointValueBefore(timer.currentTimeMillis() + 1));
            initializeIntervalLogging(timer.currentTimeMillis(), false);
        } else
            initializeIntervalLogging(0l, false);
    }

    public void terminateHistorical() {
        terminateIntervalLogging();
        pointValue.set(valueCache.getLatestPointValue());
    }

    /**
     * Get a copy of the current cache
     */
    public List<PointValueTime> getCacheCopy() {
        List<PointValueTime> cache = valueCache.getCacheContents();
        List<PointValueTime> copy = new ArrayList<>(cache.size());
        copy.addAll(cache);
        return copy;
    }

    /**
     * Get a copy of the current cache, size limited
     */
    public List<PointValueTime> getCacheCopy(int limit) {
        List<PointValueTime> cache = valueCache.getCacheContents();
        List<PointValueTime> copy = new ArrayList<>(limit);
        Iterator<PointValueTime> it = cache.iterator();
        for (int i = 0; i < limit && it.hasNext(); i++) {
            copy.add(it.next());
        }
        return copy;
    }

    @Override
    public DataPointWrapper getDataPointWrapper(AbstractPointWrapper rtWrapper) {
        return new DataPointWrapper(vo, rtWrapper);
    }

    @Override
    public String readableIdentifier() {
        return String.format("Data point (name=%s, id=%d, type=%s)", getVO().getName(), getId(), getClass().getSimpleName());
    }

    private void notifyStateChanged() {
        if (dataSource.getLifecycleState() == ILifecycleState.RUNNING) {
            dataPointDao.notifyStateChanged(getVO(), this.state);
        }
    }
}
