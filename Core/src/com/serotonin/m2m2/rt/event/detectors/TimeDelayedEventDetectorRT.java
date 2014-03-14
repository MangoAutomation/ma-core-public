/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * This is a base class for all subclasses that need to schedule timeouts for them to become active.
 * 
 * @author Matthew Lohbihler
 */
abstract public class TimeDelayedEventDetectorRT extends TimeoutDetectorRT {
    synchronized protected void scheduleJob() {
        if (getDurationMS() > 0)
            scheduleJob(System.currentTimeMillis() + getDurationMS());
        else
            // Otherwise call the event active immediately.
            setEventActive(true);
    }

    synchronized protected void unscheduleJob(long conditionInactiveTime) {
        // Reset the eventActive if it is on
        if (isEventActive())
            setEventActive(false);
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

    abstract protected long getConditionActiveTime();

    abstract void setEventActive(boolean b);

    @Override
    public void initialize() {
        super.initialize();
        initializeState();
    }

    protected void initializeState() {
        int pointId = vo.njbGetDataPoint().getId();
        PointValueTime latest = Common.runtimeManager.getDataPoint(pointId).getPointValue();

        if (latest != null)
            pointChanged(null, latest);
    }

    @Override
    public void scheduleTimeoutImpl(long fireTime) {
        setEventActive(true);
    }
}
