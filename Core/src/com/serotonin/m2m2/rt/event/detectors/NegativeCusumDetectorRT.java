/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.NegativeCusumDetectorVO;

/**
 * The NegativeCusumDetectorRT is used to detect occurances of point values below the given CUSUM limit for a given
 * duration. For example, a user may need to have an event raised when a temperature CUSUM sinks below some value for 10
 * minutes or more.
 *
 * @author Matthew Lohbihler
 */
public class NegativeCusumDetectorRT extends TimeDelayedEventDetectorRT<NegativeCusumDetectorVO> {
    private final Logger log = LoggerFactory.getLogger(NegativeCusumDetectorRT.class);

    /**
     * State field. The current negative CUSUM for the point.
     */
    private double cusum;

    /**
     * State field. Whether the negative CUSUM is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single negative CUSUM exceed.
     */
    private boolean negativeCusumActive;

    private long negativeCusumActiveTime;
    private long negativeCusumInactiveTime;

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single negative CUSUM exceed.
     */
    private boolean eventActive;

    public NegativeCusumDetectorRT(NegativeCusumDetectorVO vo) {
        super(vo);
    }

    public double getCusum() {
        return cusum;
    }

    public boolean isNegativeCusumActive() {
        return negativeCusumActive;
    }

    public long getNegativeCusumActiveTime() {
        return negativeCusumActiveTime;
    }

    public long getNegativeCusumInactiveTime() {
        return negativeCusumInactiveTime;
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyLimit = vo.getDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();
        if (durationDescription == null)
            return new TranslatableMessage("event.detector.negCusum", name, prettyLimit);
        return new TranslatableMessage("event.detector.negCusumPeriod", name, prettyLimit, durationDescription);
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * This method is only called when the negative CUSUM changes between being active or not, i.e. if the point's CUSUM
     * is currently above the limit, then it should never be called with a value of true.
     *
     * @param b
     */
    private void changeNegativeCusumActive(long timestamp) {
        negativeCusumActive = !negativeCusumActive;

        if (negativeCusumActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(timestamp);
        else
            unscheduleJob(negativeCusumInactiveTime);
    }

    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        double newDouble = newValue.getDoubleValue();
        cusum += newDouble - vo.getWeight();
        if (cusum > 0)
            cusum = 0;

        if (cusum < vo.getLimit()) {
            if (!negativeCusumActive) {
                negativeCusumActiveTime = newValue.getTime();
                changeNegativeCusumActive(time);
            }
        }
        else {
            if (negativeCusumActive) {
                negativeCusumInactiveTime = newValue.getTime();
                changeNegativeCusumActive(time);
            }
        }
    }

    @Override
    public long getConditionActiveTime() {
        return negativeCusumActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(negativeCusumInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that it is active.
        if (negativeCusumActive)
            raiseEvent(negativeCusumActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when negative cumsum detector is not active. Ignoring.");
            eventActive = false;
        }
    }

    @Override
    public String getThreadNameImpl() {
        return "NegativeCusum Detector " + this.vo.getXid();
    }

}
