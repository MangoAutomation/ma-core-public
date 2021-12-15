/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.DataPointEventNotifyWorkItem;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;

/**
 * @author Matthew Lohbihler, Terry Packer
 */
abstract public class DifferenceDetectorRT<T extends TimeoutDetectorVO<T>> extends TimeDelayedEventDetectorRT<T> {

    public DifferenceDetectorRT(T vo) {
        super(vo);
    }

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single state detection.
     */
    protected boolean eventActive;

    /**
     * Time which we last changed value
     */
    protected long lastChange;

    public long getLastChange() {
        return lastChange;
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * Received point data
     */
    synchronized protected void pointData(long fireTime) {
        if (!eventActive)
            unscheduleJob(fireTime);
        else
            setEventInactive(fireTime);
        lastChange = fireTime;
        scheduleJob(fireTime);
    }

    @Override
    public void initializeState() {
        long now = Common.timer.currentTimeMillis();
        long nextJobOffset = now;
        // Get historical data for the point out of the database.
        int pointId = vo.getDataPoint().getId();
        PointValueTime latest = Common.runtimeManager.getDataPoint(pointId).getPointValue();
        if (latest != null) {
            lastChange = latest.getTime();
            nextJobOffset = latest.getTime();
        }else {
            // The point may be new or not logged, so don't go active immediately.
            lastChange = now;
        }

        //Submit task to fire event, this will call our pointChanged(null, latest) and pointUpdated(newValue) methods in a separate thread
        //  so the raise and handle event logic is not done on the thread starting Mango.
        if (latest != null) {
            Common.backgroundProcessing.addWorkItem(new DataPointEventNotifyWorkItem(vo.getDataPoint().getXid(), this, null, latest,
                    null, false, false, false, true, false));
        }else {
            scheduleJob(nextJobOffset);
        }
    }

    @Override
    public long getConditionActiveTime() {
        return lastChange;
    }

    @Override
    public void scheduleTimeoutImpl(long fireTime) {
        //Ensure that the pointData() method hasn't updated our last change time and that we are not active already
        setEventActive(fireTime);
    }

    @Override
    public synchronized void setEventActive(long fireTime) {
        if(lastChange + getDurationMS() <= fireTime) {
            eventActive = true;
            raiseEvent(fireTime, createEventContext());
        }
    }

    @Override
    protected void setEventInactive(long timestamp) {
        eventActive = false;
        returnToNormal(timestamp);
    }
}
