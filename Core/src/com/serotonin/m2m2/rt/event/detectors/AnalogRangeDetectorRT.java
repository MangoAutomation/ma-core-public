/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.AnalogRangeDetectorVO;

/**
 * The AnalogRangeDetector is used to detect occurrences of point values within/outside the given range for a given
 * duration. For example, a user may need to have an event raised when a temperature is within some range for 10 minutes
 * or more.
 *
 * This detector overrides the limit (for high end) and weight (for low end) range values thus the range is defined as:
 * weight <= value <= limit
 *
 * This detector overrides the binaryState values to indicate within or outside of the range detection
 *
 *
 * The configuration fields provided are static for the lifetime of this detector. The state fields vary based on the
 * changing conditions in the system. In particular, the highLimitActive field describes whether the point's value is
 * currently above the high limit or not. The eventActive field describes whether the point's value has been above the
 * high limit for longer than the tolerance duration.
 *
 * @author Terry Packer
 *
 */
public class AnalogRangeDetectorRT extends TimeDelayedEventDetectorRT<AnalogRangeDetectorVO> {
    private final Log log = LogFactory.getLog(AnalogRangeDetectorRT.class);

    /**
     * State field. Whether the range is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single high limit exceed.
     */
    private boolean rangeActive;
    private long rangeActiveTime;
    private long rangeInactiveTime;

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single range event.
     */
    private boolean eventActive;

    public AnalogRangeDetectorRT(AnalogRangeDetectorVO vo) {
        super(vo);
    }

    public boolean isRangeActive() {
        return rangeActive;
    }

    public long getRangeActiveTime() {
        return rangeActiveTime;
    }

    public long getRangeInactiveTime() {
        return rangeInactiveTime;
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyHighRange = vo.getDataPoint().getTextRenderer().getText(vo.getHigh(), TextRenderer.HINT_SPECIFIC);
        String prettyLowRange = vo.getDataPoint().getTextRenderer().getText(vo.getLow(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();

        if(vo.isWithinRange()){
            //Return message for within range
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.range", name, prettyLowRange, prettyHighRange);
            return new TranslatableMessage("event.detector.rangePeriod", name, prettyLowRange, prettyHighRange, durationDescription);

        }else{
            //Return message for outside range
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.rangeOutside", name, prettyLowRange, prettyHighRange);
            return new TranslatableMessage("event.detector.rangeOutsidePeriod", name, prettyLowRange, prettyHighRange, durationDescription);

        }
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * This method is only called when the high limit changes between being active or not, i.e. if the point's value is
     * currently above the high limit, then it should never be called with a value of true.
     */
    private void changeRangeActive(long time) {
        rangeActive = !rangeActive;

        if (rangeActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(rangeInactiveTime);
    }

    @Override
    synchronized public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        double newDouble = newValue.getDoubleValue();
        //Are we supposed to be within or outside?
        if(vo.isWithinRange()){
            //Fire event if within range
            if ((newDouble <= vo.getHigh())&&(newDouble >= vo.getLow())) {
                if (!rangeActive) {
                    rangeActiveTime = newValue.getTime();
                    changeRangeActive(time);
                }
            }
            else {
                if (rangeActive) {
                    rangeInactiveTime = newValue.getTime();
                    changeRangeActive(time);
                }
            }
        }else{
            //Fire event if Outside Range detection
            if ((newDouble >= vo.getHigh())||(newDouble <= vo.getLow())) {
                if (!rangeActive) {
                    rangeActiveTime = newValue.getTime();
                    changeRangeActive(time);
                }
            }
            else {
                if (rangeActive) {
                    rangeInactiveTime = newValue.getTime();
                    changeRangeActive(time);
                }
            }
        }
    }

    @Override
    public long getConditionActiveTime() {
        return rangeActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(rangeInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that the high limit is active.
        if (rangeActive)
            raiseEvent(rangeActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when range detector is not active. Ignoring.");
            eventActive = false;
        }
    }

    @Override
    public String getThreadNameImpl() {
        return "AnalogLowLimit Detector " + this.vo.getXid();
    }

}
