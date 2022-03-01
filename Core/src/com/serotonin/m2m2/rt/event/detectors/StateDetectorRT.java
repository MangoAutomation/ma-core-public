/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;

/**
 * @author Matthew Lohbihler
 */
abstract public class StateDetectorRT<T extends TimeoutDetectorVO<T>> extends TimeDelayedEventDetectorRT<T> {

    private int riseEventCounter=0;

    /**
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

    private ConcurrentLinkedQueue<Long> raisingEventsQueue = new ConcurrentLinkedQueue<Long>();

    private final Object LOCK = new Object();

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

        synchronized (LOCK) {
            stateActive = !stateActive;
            if (stateActive)
                // Schedule a job that will call the event active if it runs.
                scheduleJob(time);
            else
                unscheduleJob(stateInactiveTime);
        }

    }

    abstract protected boolean stateDetected(PointValueTime newValue);

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        if (stateDetected(newValue)) {
            raisingEventsQueue.add(time);
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
        synchronized (LOCK){
            raisingEventsQueue.remove(timestamp);
            this.eventActive = false;
            returnToNormal(stateInactiveTime);
        }

    }

    @Override
    protected void setEventActive(long timestamp) {
        synchronized (LOCK){

            //TODO: @BPM The event should be raised here regardless of the stateActive?
            //raiseEvent(stateActiveTime + getDurationMS(), createEventContext());
            //this.eventActive = true;

            // Just for the fun of it, make sure that the high limit is active.
            if (stateActive){
                //do we really want to re-raise the event?
                this.eventActive = true;
                //raiseEvent(stateActiveTime + getDurationMS(), createEventContext());
                raiseEvent(timestamp, createEventContext());
                raisingEventsQueue.remove(timestamp);
            }
            else {
                if(!raisingEventsQueue.isEmpty()){
                    Long time = raisingEventsQueue.remove();
                    if(time != null){ //there was an event in the queue
                        raiseEvent(timestamp, createEventContext());
                        //setEventInactive(timer.currentTimeMillis());
                    }
                }
                // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
                if(eventActive){
                    setEventInactive(timestamp);
                }else
                    log.warn("Call to set event active when state detector is not active. Ignoring.");

            }
        }
    }
}
