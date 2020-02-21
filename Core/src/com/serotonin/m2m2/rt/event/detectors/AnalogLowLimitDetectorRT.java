/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.AnalogLowLimitDetectorVO;

/**
 * The AnalogLowLimitDetector is used to detect occurances of point values below the given low limit for a given
 * duration. For example, a user may need to have an event raised when a temperature sinks below some value for 10
 * minutes or more. Or a user may need to have an event raised when a temperature does not sink below some value
 * for 10 minutes.
 *
 * Additionally the vo.weight parameter is used as a threshold for turning off the detector and the multistateState value
 * to determine if we are using the threshold
 *
 * The configuration fields provided are static for the lifetime of this detector. The state fields vary based on the
 * changing conditions in the system. In particular, the lowLimitActive field describes whether the point's value is
 * currently below the low limit or not. The eventActive field describes whether the point's value has been below the
 * low limit for longer than the tolerance duration.
 *
 * @author Matthew Lohbihler
 */
public class AnalogLowLimitDetectorRT extends TimeDelayedEventDetectorRT<AnalogLowLimitDetectorVO> {
    private final Log log = LogFactory.getLog(AnalogLowLimitDetectorRT.class);

    /**
     * State field. Whether the low limit is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single low limit event.
     */
    private boolean lowLimitActive;
    private long lowLimitActiveTime;
    private long lowLimitInactiveTime;

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single low limit event.
     */
    private boolean eventActive;

    public AnalogLowLimitDetectorRT(AnalogLowLimitDetectorVO vo) {
        super(vo);
    }

    public boolean isLowLimitActive() {
        return lowLimitActive;
    }

    public long getLowLimitActiveTime() {
        return lowLimitActiveTime;
    }

    public long getLowLimitInactiveTime() {
        return lowLimitInactiveTime;
    }

    @Override
    public TranslatableMessage getMessage() {
        TranslatableMessage durationDescription = getDurationDescription();
        String name = vo.getDataPoint().getExtendedName();
        String prettyLimit = vo.getDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);

        if(vo.isNotLower()){
            //Is not lower
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.lowLimitNotLower", name, prettyLimit);
            return new TranslatableMessage("event.detector.lowLimitNotLowerPeriod", name, prettyLimit, durationDescription);
        }else{
            //is lower
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.lowLimit", name, prettyLimit);
            return new TranslatableMessage("event.detector.lowLimitPeriod", name, prettyLimit, durationDescription);
        }
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * This method is only called when the low limit changes between being active or not, i.e. if the point's value is
     * currently below the low limit, then it should never be called with a value of true.
     *
     */
    private void changeLowLimitActive(long time) {
        lowLimitActive = !lowLimitActive;

        if (lowLimitActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(lowLimitInactiveTime);
    }

    @Override
    synchronized public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        double newDouble = newValue.getDoubleValue();
        if(vo.isNotLower()){
            //Not Lower than
            if (newDouble >= vo.getLimit()) {
                if (!lowLimitActive) {
                    lowLimitActiveTime = newValue.getTime();
                    changeLowLimitActive(time);
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetLimit()){
                    if ((lowLimitActive)&&(newDouble <= vo.getResetLimit())) {
                        lowLimitInactiveTime = newValue.getTime();
                        changeLowLimitActive(time);
                    }
                }else{
                    if (lowLimitActive) {
                        lowLimitInactiveTime = newValue.getTime();
                        changeLowLimitActive(time);
                    }
                }
            }
        }else{
            //is lower than
            if (newDouble < vo.getLimit()) {
                if (!lowLimitActive) {
                    lowLimitActiveTime = newValue.getTime();
                    changeLowLimitActive(time);
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetLimit()){
                    if ((lowLimitActive)&&(newDouble >= vo.getResetLimit())) {
                        lowLimitInactiveTime = newValue.getTime();
                        changeLowLimitActive(time);
                    }
                }else{
                    if (lowLimitActive) {
                        lowLimitInactiveTime = newValue.getTime();
                        changeLowLimitActive(time);
                    }
                }
            }
        }
    }

    @Override
    public long getConditionActiveTime() {
        return lowLimitActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(lowLimitInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that the high limit is active.
        if (lowLimitActive)
            raiseEvent(lowLimitActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when low limit detector is not active. Ignoring.");
            eventActive = false;
        }
    }

    @Override
    public String getThreadNameImpl() {
        return "AnalogLowLimit Detector " + this.vo.getXid();
    }

}
