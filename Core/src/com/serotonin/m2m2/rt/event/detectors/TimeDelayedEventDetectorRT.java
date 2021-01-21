/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

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
     * @param vo
     */
    public TimeDelayedEventDetectorRT(T vo) {
        super(vo);
    }

    /**
     * Schedule a job passing in the time of now for reference
     */
    @Override
    synchronized protected void scheduleJob(long now) {
        if (getDurationMS() > 0)
            super.scheduleJob(now + getDurationMS());
        else if(!isEventActive())
            setEventActive(now);
    }

    /**
     * Unschedule a job,
     *  - set event inactive if its active
     *  - raise and RTN an event in the past if it is inactive now
     * @param conditionInactiveTime
     */
    synchronized protected void unscheduleJob(long conditionInactiveTime) {
        // Reset the eventActive if it is on
        if (isEventActive())
            setEventInactive(conditionInactiveTime);

        // Check whether there is a tolerance duration.
        else if (getDurationMS() > 0) {
            if (isJobScheduled()) {
                unscheduleJob();

                // There is an existing job scheduled. It's fire time is likely past when the event is to actually fire,
                // so check if the event activation time
                long eventActiveTime = getConditionActiveTime() + getDurationMS();

                if (eventActiveTime < conditionInactiveTime) {
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
     * @return
     */
    abstract public long getConditionActiveTime();

    /**
     * Change the state of the event, raise using the supplied timestamp if necessary
     * @param timestamp
     */
    protected abstract void setEventActive(long timestamp);

    /**
     * Change the state of the event, rtn using the supplied timestamp if necessary
     * @param timestamp
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
