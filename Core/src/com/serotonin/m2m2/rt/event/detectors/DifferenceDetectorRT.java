/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;

/**
 * @author Matthew Lohbihler
 */
abstract public class DifferenceDetectorRT<T extends TimeoutDetectorVO<T>> extends TimeDelayedEventDetectorRT<T> {
    /**
	 * @param vo
	 */
	public DifferenceDetectorRT(T vo) {
		super(vo);
	}

	/**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single state detection.
     */
    protected boolean eventActive;

    protected long lastChange;

    @Override
	public boolean isEventActive() {
        return eventActive;
    }

    synchronized protected void pointData() {
    	lastChange = Common.timer.currentTimeMillis();
    	if (!eventActive)
            unscheduleJob(Common.timer.currentTimeMillis());
        else
            setEventActive(false);
        
        scheduleJob();
    }

    @Override
    public void initializeState() {
        // Get historical data for the point out of the database.
        int pointId = vo.getDataPoint().getId();
        PointValueTime latest = Common.runtimeManager.getDataPoint(pointId).getPointValue();
        if (latest != null)
            lastChange = latest.getTime();
        else
            // The point may be new or not logged, so don't go active immediately.
            lastChange = Common.timer.currentTimeMillis();

        if (lastChange + getDurationMS() < Common.timer.currentTimeMillis())
            // Nothing has happened in the time frame, so set the event active.
            setEventActive(true);
        else
            // Otherwise, set the timeout.
            scheduleJob();
    }

    @Override
    protected long getConditionActiveTime() {
        return lastChange;
    }

    @Override
    synchronized public void setEventActive(boolean b) {
        eventActive = b;
        if (eventActive)
            // Raise the event.
            raiseEvent(lastChange + getDurationMS(), createEventContext());
        else
            // Deactivate the event.
            returnToNormal(lastChange);
    }
}
