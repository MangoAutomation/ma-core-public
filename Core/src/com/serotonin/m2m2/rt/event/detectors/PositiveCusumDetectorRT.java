/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.PositiveCusumDetectorVO;

/**
 * The PositiveCusumDetector is used to detect occurrences of point values exceeding the given CUSUM limit for a given
 * duration. For example, a user may need to have an event raised when a temperature CUSUM exceeds some value for 10
 * minutes or more.
 *
 * @author Matthew Lohbihler
 */
public class PositiveCusumDetectorRT extends TimeDelayedEventDetectorRT<PositiveCusumDetectorVO> {
    private final Log log = LogFactory.getLog(PositiveCusumDetectorRT.class);
    /**
     * State field. The current positive CUSUM for the point.
     */
    private double cusum;

    /**
     * State field. Whether the positive CUSUM is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single positive CUSUM exceed.
     */
    private boolean positiveCusumActive;

    private long positiveCusumActiveTime;
    private long positiveCusumInactiveTime;

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single positive CUSUM exceed.
     */
    private boolean eventActive;

    public PositiveCusumDetectorRT(PositiveCusumDetectorVO vo) {
        super(vo);
    }

    public double getCusum() {
        return cusum;
    }

    public boolean isPositiveCusumActive() {
        return positiveCusumActive;
    }

    public long getPositiveCusumActiveTime() {
        return positiveCusumActiveTime;
    }

    public long getPositiveCusumInactiveTime() {
        return positiveCusumInactiveTime;
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyLimit = vo.getDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();
        if (durationDescription == null)
            return new TranslatableMessage("event.detector.posCusum", name, prettyLimit);
        return new TranslatableMessage("event.detector.posCusumPeriod", name, prettyLimit, durationDescription);
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * This method is only called when the positive CUSUM changes between being active or not, i.e. if the point's CUSUM
     * is currently above the limit, then it should never be called with a value of true.
     *
     * @param b
     */
    private void changePositiveCusumActive(long time) {
        positiveCusumActive = !positiveCusumActive;

        if (positiveCusumActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(positiveCusumInactiveTime);
    }

    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        double newDouble = newValue.getDoubleValue();
        cusum += newDouble - vo.getWeight();
        if (cusum < 0)
            cusum = 0;

        if (cusum > vo.getLimit()) {
            if (!positiveCusumActive) {
                positiveCusumActiveTime = newValue.getTime();
                changePositiveCusumActive(time);
            }
        }
        else {
            if (positiveCusumActive) {
                positiveCusumInactiveTime = newValue.getTime();
                changePositiveCusumActive(time);
            }
        }
    }

    @Override
    public long getConditionActiveTime() {
        return positiveCusumActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(positiveCusumInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that the high limit is active.
        if (positiveCusumActive)
            raiseEvent(positiveCusumActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when positive cusum detector is not active. Ignoring.");
            eventActive = false;
        }
    }

    @Override
    public String getThreadNameImpl() {
        return "PosCusumDetector " + this.vo.getXid();
    }

}
