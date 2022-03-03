/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.Date;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;

/**
 * @author Matthew Lohbihler
 */
abstract public class StateDetectorRT<T extends TimeoutDetectorVO<T>> extends TimeDelayedEventDetectorRT<T> {

    /**
     */
    public StateDetectorRT(T vo) {
        super(vo);
    }

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

        if(log.isTraceEnabled()) {
            log.trace("changeStateActive({}) {}", new Date(time), stateActive);
        }

        if (stateActive) {
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        }else {
            unscheduleJob(stateInactiveTime);
        }
    }

    abstract protected boolean stateDetected(PointValueTime newValue);

    @Override
    public synchronized void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        //TODO what time to use here?  timer or newValue.getTime()?
        //long time = Common.timer.currentTimeMillis();
        long time = newValue.getTime();

        if(log.isTraceEnabled()) {
            log.trace("pointChanged({}) at {}", newValue.getIntegerValue(), new Date(time));
        }

        if (stateDetected(newValue)) {
            if (!stateActive) {
                stateActiveTime = time;
                changeStateActive(time);
            }
        }
        else {
            if (stateActive) {
                stateInactiveTime = time;
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
        if(log.isTraceEnabled()) {
            log.trace("setEventInactive({})", new Date(timestamp));
        }
        this.eventActive = false;
        returnToNormal(stateInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        if(log.isTraceEnabled()) {
            log.trace("setEventActive({})", new Date(timestamp));
        }

        // Just for the fun of it, make sure that the high limit is active.
        if (stateActive) {
            raiseEvent(stateActiveTime + getDurationMS(), createEventContext());
            this.eventActive = true;
        }else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when state detector is not active. Ignoring.");
        }
    }
}
