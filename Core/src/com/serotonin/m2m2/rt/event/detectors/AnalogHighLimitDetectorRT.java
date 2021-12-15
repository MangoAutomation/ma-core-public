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
import com.serotonin.m2m2.vo.event.detector.AnalogHighLimitDetectorVO;

/**
 * The AnalogHighLimitDetector is used to detect occurrences of point values exceeding the given high limit for a given
 * duration. For example, a user may need to have an event raised when a temperature exceeds some value for 10 minutes
 * or more. Or a user may need to have an event raised when a temperature has not exceeded some value for 10 minutes.
 *
 * Additionally the vo.weight parameter is used as a threshold for turning off the detector and the multistateState value
 * to determine if we are using the threshold
 *
 * The configuration fields provided are static for the lifetime of this detector. The state fields vary based on the
 * changing conditions in the system. In particular, the highLimitActive field describes whether the point's value is
 * currently above the high limit or not. The eventActive field describes whether the point's value has been above the
 * high limit for longer than the tolerance duration.
 *
 * @author Matthew Lohbihler
 */
public class AnalogHighLimitDetectorRT extends TimeDelayedEventDetectorRT<AnalogHighLimitDetectorVO> {
    private final Logger log = LoggerFactory.getLogger(AnalogHighLimitDetectorRT.class);

    /**
     * State field. Whether the high limit is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single high limit exceed.
     */
    private boolean highLimitActive;
    private long highLimitActiveTime;
    private long highLimitInactiveTime;

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single high limit exceed.
     */
    private boolean eventActive;

    public AnalogHighLimitDetectorRT(AnalogHighLimitDetectorVO vo) {
        super(vo);
    }

    public boolean isHighLimitActive() {
        return highLimitActive;
    }

    public long getHighLimitActiveTime() {
        return highLimitActiveTime;
    }

    public long getHighLimitInactiveTime() {
        return highLimitInactiveTime;
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyLimit = vo.getDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();

        if(vo.isNotHigher()){
            //Not Higher than
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.highLimitNotHigher", name, prettyLimit);
            return new TranslatableMessage("event.detector.highLimitNotHigherPeriod", name, prettyLimit, durationDescription);
        }else{
            //Higher than
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.highLimit", name, prettyLimit);
            return new TranslatableMessage("event.detector.highLimitPeriod", name, prettyLimit, durationDescription);
        }
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * This method is only called when the high limit changes between being active or not, i.e. if the point's value is
     * currently above the high limit, then it should never be called with a value of true.
     *
     */
    private void changeHighLimitActive(long time) {
        highLimitActive = !highLimitActive;
        if (highLimitActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(highLimitInactiveTime);
    }

    @Override
    synchronized public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        double newDouble = newValue.getDoubleValue();
        if(vo.isNotHigher()){
            //Is Not Higher
            if (newDouble <= vo.getLimit()) {
                if (!highLimitActive) {
                    highLimitActiveTime = newValue.getTime();
                    changeHighLimitActive(time);
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetLimit()){
                    if ((highLimitActive)&&(newDouble >= vo.getResetLimit())) {
                        highLimitInactiveTime = newValue.getTime();
                        changeHighLimitActive(time);
                    }
                }else{
                    //Not using reset
                    if (highLimitActive) {
                        highLimitInactiveTime = newValue.getTime();
                        changeHighLimitActive(time);
                    }
                }
            }
        }else{
            //Is Higher
            if (newDouble > vo.getLimit()) {
                if (!highLimitActive) {
                    highLimitActiveTime = newValue.getTime();
                    changeHighLimitActive(time);
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetLimit()){
                    //Turn off alarm if we are active and below the Reset value
                    if ((highLimitActive) &&(newDouble <= vo.getResetLimit())){
                        highLimitInactiveTime = newValue.getTime();
                        changeHighLimitActive(time);
                    }
                }else{
                    //Turn off alarm if we are active
                    if (highLimitActive){
                        highLimitInactiveTime = newValue.getTime();
                        changeHighLimitActive(time);
                    }
                }
            }
        }
    }

    @Override
    public long getConditionActiveTime() {
        return highLimitActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(highLimitInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that the high limit is active.
        if (highLimitActive)
            raiseEvent(highLimitActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when high limit detector is not active. Ignoring.");
            eventActive = false;
        }
    }

    @Override
    public String getThreadNameImpl() {
        return "AnalogHighLimit Detector " + this.vo.getXid();
    }

}
