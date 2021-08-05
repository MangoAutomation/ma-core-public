/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.SmoothnessDetectorVO;
import com.serotonin.util.queue.ObjectQueue;

/**
 * The SmoothnessDetectorRT is used to detect erratic input from what should otherwise be a stable sensor. This
 * was developed specifically for a model of temperature sensors used in brewing vats, but can be used in other
 * situations as well.
 *
 * @author Matthew Lohbihler
 */
public class SmoothnessDetectorRT extends TimeDelayedEventDetectorRT<SmoothnessDetectorVO> {
    private final Logger log = LoggerFactory.getLogger(SmoothnessDetectorRT.class);

    /**
     * State field. The current boxcar.
     */
    private final ObjectQueue<Double> boxcar = new ObjectQueue<>();

    /**
     * State field. Whether the smoothness is currently below the limit or not. This field is used to prevent multiple
     * events being raised during the duration of a single limit breech.
     */
    private boolean limitBreech;
    private long limitBreechActiveTime;
    private long limitBreechInactiveTime;

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single limit breech.
     */
    private boolean eventActive;

    public SmoothnessDetectorRT(SmoothnessDetectorVO vo) {
        super(vo);
    }

    public ObjectQueue<Double> getBoxcar() {
        synchronized(boxcar) {
            ObjectQueue<Double> temp = new ObjectQueue<>(boxcar.size());
            for (Double value : boxcar) {
                temp.push(value);
            }
            return temp;
        }
    }
    public boolean isLimitBreech() {
        return limitBreech;
    }

    public long getLimitBreechActiveTime() {
        return limitBreechActiveTime;
    }

    public long getLimitBreechInactiveTime() {
        return limitBreechInactiveTime;
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyLimit = vo.getDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();
        if (durationDescription == null)
            return new TranslatableMessage("event.detector.smoothness", name, prettyLimit);
        return new TranslatableMessage("event.detector.smoothnessPeriod", name, prettyLimit, durationDescription);
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * This method is only called when the smoothness crossed the configured limit in either direction.
     */
    private void changeLimitBreech(long time) {
        limitBreech = !limitBreech;

        if (limitBreech)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(limitBreechInactiveTime);
    }

    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        long time = Common.timer.currentTimeMillis();
        double newDouble = newValue.getDoubleValue();
        double smoothness;

        synchronized(boxcar) {
            // Add the value to the boxcar.
            boxcar.push(newDouble);

            // Trim the boxcar to the max size
            while (boxcar.size() > vo.getBoxcar())
                boxcar.pop();

            // Calculate the smoothness
            smoothness = calc();
        }

        if (smoothness < vo.getLimit()) {
            if (!limitBreech) {
                limitBreechActiveTime = newValue.getTime();
                changeLimitBreech(time);
            }
        }
        else {
            if (limitBreech) {
                limitBreechInactiveTime = newValue.getTime();
                changeLimitBreech(time);
            }
        }
    }

    @Override
    public long getConditionActiveTime() {
        return limitBreechActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(limitBreechInactiveTime);
    }

    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that the high limit is active.
        if (limitBreech)
            raiseEvent(limitBreechActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when smootheness detector is not active. Ignoring.");
            eventActive = false;
        }
    }

    private double calc() {
        if (boxcar.size() < 3)
            return 1;

        double prev = Double.NaN;
        double lastAngle = Double.NaN;
        double sumErr = 0;
        int count = 0;

        for (Double value : boxcar) {
            if (!Double.isNaN(prev)) {
                double opp = value - prev;
                double hyp = StrictMath.sqrt(0.1 + opp * opp);
                double angle = StrictMath.asin(opp / hyp);

                if (!Double.isNaN(lastAngle)) {
                    double diff = (angle - lastAngle);
                    double norm = diff / Math.PI;
                    sumErr += norm < 0 ? -norm : norm;
                    count++;
                }

                lastAngle = angle;
            }

            prev = value;
        }

        double err = sumErr / count;
        return (float) (1 - err);
    }

    @Override
    public String getThreadNameImpl() {
        return "Smoothness Detector " + this.vo.getXid();
    }

}
