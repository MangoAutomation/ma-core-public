/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.DataPointEventNotifyWorkItem;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;

/**
 * This is a base class for all subclasses that need to schedule timeouts for them to become active.
 *
 * @author Matthew Lohbihler
 */
abstract public class TimeDelayedEventDetectorRT<T extends TimeoutDetectorVO<T>> extends TimeoutDetectorRT<T> {

    private final Logger log = LoggerFactory.getLogger(TimeDelayedEventDetectorRT.class);

    private final Object LOCK = new Object();

    /**
     */
    public TimeDelayedEventDetectorRT(T vo) {
        super(vo);
    }

    /**
     * Schedule a job passing in the time of now for reference
     */
    @Override
    protected void scheduleJob(long now) {
        synchronized (LOCK) {
            if (getDurationMS() > 0){

                    super.scheduleJob(now + getDurationMS());
            } else if(!isEventActive()){
                setEventActive(now);
            }
        }
    }

    /**
     * Unschedule a job,
     *  - set event inactive if its active
     *  - raise and RTN an event in the past if it is inactive now
     * @return
     */
    protected void unscheduleJob(long conditionInactiveTime) {
        // Reset the eventActive if it is on
        if (isEventActive()){
            setEventInactive(conditionInactiveTime);
        }
        // Check whether there is a tolerance duration.
        else if (getDurationMS() > 0) {

            if (isJobScheduled()) {
                synchronized (LOCK) {
                    if(isJobScheduled()) {
                        if (!isEventActive())
                            unscheduleJob();
                        // There is an existing job scheduled. It's fire time is likely past when the event is to actually fire,
                        // so check if the event activation time
                        long eventActiveTime = getConditionActiveTime() + getDurationMS();

                        if (eventActiveTime < conditionInactiveTime) {
                            // The event should go active.
                            if (log.isWarnEnabled())
                                log.warn("Calling raiseEvent and returnToNormal immediately after");

                            //raiseEvent(eventActiveTime, createEventContext());
                            setEventActive(eventActiveTime);

                            // And then go inactive
                            //returnToNormal(conditionInactiveTime);
                            setEventInactive(conditionInactiveTime);

                        }
                    }
                }
            }
        }
    }

    /**
     * The timestamp for when the condition has gone active
     */
    abstract public long getConditionActiveTime();

    /**
     * Change the state of the event, raise using the supplied timestamp if necessary
     */
    protected abstract void setEventActive(long timestamp);

    /**
     * Change the state of the event, rtn using the supplied timestamp if necessary
     */
    protected abstract void setEventInactive(long timestamp);


    @Override
    public void initialize() {
        super.initialize();
        initializeState();
    }

    /**
     * Initialize the state of the detector
     */
    protected void initializeState() {
        int pointId = vo.getDataPoint().getId();
        PointValueTime latest = Common.runtimeManager.getDataPoint(pointId).getPointValue();

        //Submit task to fire event, this will call our pointChanged(null, latest) and pointUpdated(newValue) methods in a separate thread
        //  so the raise and handle event logic is not done on the thread starting Mango.
        if (latest != null) {
            Common.backgroundProcessing.addWorkItem(new DataPointEventNotifyWorkItem(vo.getDataPoint().getXid(), this, null, latest,
                    null, false, false, false, true, false));
        }
    }

    @Override
    public void scheduleTimeoutImpl(long fireTime) {
        setEventActive(fireTime);
    }
}
