/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;

/**
 * @author Matthew Lohbihler
 */
abstract public class StateDetectorRT<T extends TimeoutDetectorVO<T>> extends TimeDelayedEventDetectorRT<T> {

    /**
     * @param vo
     */
    public StateDetectorRT(T vo) {
        super(vo);
    }

    private final Logger log = LoggerFactory.getLogger(StateDetectorRT.class);

    /**
     * State field. Whether the state has been detected or not. This field is used to prevent multiple events being
     * raised during the duration of a single state detection.
     */
    private boolean stateActive;

    private long stateActiveTime;
    private long stateInactiveTime;

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single state detection.
     */
    private boolean eventActive;

    protected boolean isActive() {
        return eventActive;
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    public boolean isStateActive() {
        return stateActive;
    }

    public long getStateActiveTime() {
        return stateActiveTime;
    }

    public long getStateInactiveTime() {
        return stateInactiveTime;
    }

    private void changeStateActive(long time) {
        stateActive = !stateActive;

        if (stateActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(stateInactiveTime);
    }

    abstract protected boolean stateDetected(PointValueTime newValue);

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        if (stateDetected(newValue)) {
            if (!stateActive) {
                stateActiveTime = newValue.getTime();
                changeStateActive(time);
            }
        }
        else {
            if (stateActive) {
                stateInactiveTime = newValue.getTime();
                changeStateActive(time);
            }
        }
    }

    @Override
    public long getConditionActiveTime() {
        return stateActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(stateInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that the high limit is active.
        if (stateActive)
            raiseEvent(stateActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when state detector is not active. Ignoring.");
            eventActive = false;
        }
    }
}
