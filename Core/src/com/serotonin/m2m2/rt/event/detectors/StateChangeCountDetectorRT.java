/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.StateChangeCountDetectorVO;

public class StateChangeCountDetectorRT extends TimeoutDetectorRT<StateChangeCountDetectorVO> {
    private final Logger log = LoggerFactory.getLogger(StateChangeCountDetectorRT.class);

    /**
     * State field. The point values that have accumulated so far. Each call to pointChanged will drop off the values
     * beyond the duration.
     */
    private final List<PointValueTime> pointValues = new LinkedList<PointValueTime>();

    /**
     * State field. Whether the state has been detected or not. This field is used to prevent multiple events being
     * raised during the duration of a single state detection.
     */
    private boolean eventActive;
    private long eventActiveTime;

    public StateChangeCountDetectorRT(StateChangeCountDetectorVO vo) {
        super(vo);
    }

    public List<PointValueTime> getPointValues() {
        synchronized(pointValues) {
            return new ArrayList<>(pointValues);
        }
    }

    public long getEventActiveTime() {
        return eventActiveTime;
    }

    @Override
    public TranslatableMessage getMessage() {
        return new TranslatableMessage("event.detector.changeCount", vo.getDataPoint().getExtendedName(),
                vo.getChangeCount(), getDurationDescription());
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    @Override
    public void initialize() {
        super.initialize();

        // This code produces false positives when logging on the point is set to all data, and false negatives
        // when logging is set to no data. Best to just not even try.

        // // Get historical data for the point out of the database.
        // List<PointValueTime> data = new PointValueDao().getPointValues(dataPoint, then);
        //
        // List<PointValueTime> history = Common.ctx.getRuntimeManager().getDataPoint(
        // vo.njbGetDataPoint().getId()).getLatestPointValues(vo.getChangeCount());
        //
        // PointValueTime lastValue = null;
        // PointValueTime thisValue = null;
        // for (int i=history.size() - 1; i>0; i--) {
        // thisValue = history.get(i);
        // // Only add values that are changes.
        // if (!PointValueTime.equalValues(lastValue, thisValue)) {
        // pointValues.add(thisValue);
        // lastValue = thisValue;
        // }
        // }
        //
        // if (history.size() > 0)
        // pointChanged(Common.timer.currentTimeMillis(), history.get(0));
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        pointChanged(newValue.getTime(), newValue);
    }

    private void pointChanged(long time, PointValueTime value) {
        synchronized (pointValues) {
            // Add the new change
            pointValues.add(value);

            // Drop off point values beyond the given time frame.
            removeOldPointValues(time);

            // Check if we're past the change limit.
            if (pointValues.size() >= vo.getChangeCount()) {
                if (!eventActive) {
                    eventActive = true;
                    eventActiveTime = value.getTime();

                    // Raise the event.
                    raiseEvent(eventActiveTime, createEventContext());
                }
                else
                    // We have already scheduled a timeout, so remove it
                    unscheduleJob();

                // Schedule a job for the deactivation of this detector.
                long eventInactiveTime = pointValues.get(pointValues.size() - vo.getChangeCount()).getTime()
                        + getDurationMS();
                scheduleJob(eventInactiveTime + 1);
            }
        }
    }

    @Override
    public void scheduleTimeoutImpl(long fireTime) {
        synchronized (pointValues) {
            // This call was scheduled to occur at the eventInactiveTime.
            // Strictly speaking, the fact this method was called implies that the detector is going from active to
            // inactive. However, it really doesn't hurt to do a bit of cleanup and checking, so what the heck...
            removeOldPointValues(fireTime);

            if (pointValues.size() >= vo.getChangeCount()) {
                // Something has gone wrong.
                StringBuilder sb = new StringBuilder();
                sb.append("I was supposed to go inactive, but there are still too many state changes in my list: ");
                sb.append("fireTime=").append(fireTime);
                sb.append(", list=[");
                for (PointValueTime pvt : pointValues)
                    sb.append(pvt.getTime()).append(", ");
                sb.append("], durationMS=").append(getDurationMS());
                sb.append(", changeCount=").append(vo.getChangeCount());
                log.error(sb.toString(), new Exception());
            }
        }

        // Deactive the event.
        eventActive = false;
        returnToNormal(fireTime);
    }

    private void removeOldPointValues(long time) {
        while (pointValues.size() > 0) {
            if (pointValues.get(0).getTime() < time - getDurationMS())
                pointValues.remove(0);
            else
                break;
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
     */
    @Override
    public String getThreadNameImpl() {
        return "StateChangeCounter " + this.vo.getXid();
    }

}
