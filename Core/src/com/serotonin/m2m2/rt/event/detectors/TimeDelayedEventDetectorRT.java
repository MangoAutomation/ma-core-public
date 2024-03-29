/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.Date;

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

    /**
     */
    public TimeDelayedEventDetectorRT(T vo) {
        super(vo);
    }

    /**
     * Schedule a job passing in the time of now for reference
     */
    @Override
    synchronized protected void scheduleJob(long now) {
        if(log.isTraceEnabled()) {
            log.trace("scheduleJob({})", new Date(now));
        }
        if (getDurationMS() > 0)
            super.scheduleJob(now + getDurationMS());
        else if(!isEventActive())
            setEventActive(now);
    }

    /**
     * Unschedule a job,
     *  - set event inactive if its active
     *  - raise and RTN an event in the past if it is inactive now
     */
    synchronized protected void unscheduleJob(long conditionInactiveTime) {
        if(log.isTraceEnabled()) {
            log.trace("unscheduleJob({})", new Date(conditionInactiveTime));
        }

        // Reset the eventActive if it is on
        if (isEventActive()) {
            setEventInactive(conditionInactiveTime);
        }else if (getDurationMS() > 0) { // Check whether there is a tolerance duration.

            if (isJobScheduled()) {
                unscheduleJob();

                // There is an existing job scheduled. It's fire time is likely past when the event is to actually fire,
                // so check if the event activation time
                long eventActiveTime = getConditionActiveTime() + getDurationMS();

                if (eventActiveTime <= conditionInactiveTime) {
                    // The event should go active.
                    raiseEvent(eventActiveTime, createEventContext());
                    // And then go inactive
                    returnToNormal(conditionInactiveTime);
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
    public synchronized void scheduleTimeoutImpl(long fireTime) {
        //We were cancelled but already submitted to run
        if(task == null) {
            if(log.isTraceEnabled()) {
                log.trace("scheduleTimeout({}) - job already cancelled, aborting", new Date(fireTime));
            }
            return;
        }

        if(log.isTraceEnabled()) {
            log.trace("scheduleTimeoutImpl({})", new Date(fireTime));
        }
        setEventActive(fireTime);
    }
}
