/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.SmoothnessDetectorVO;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.queue.ObjectQueue;

/**
 * The SmoothnessDetectorRT is used to detect erratic input from what should otherwise be a stable sensor. This
 * was developed specifically for a model of temperature sensors used in brewing vats, but can be used in other
 * situations as well.
 * 
 * @author Matthew Lohbihler
 */
public class SmoothnessDetectorRT extends TimeDelayedEventDetectorRT<SmoothnessDetectorVO> {
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

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.njbGetDataPoint().getName();
        String prettyLimit = vo.njbGetDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
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
    private void changeLimitBreech() {
        limitBreech = !limitBreech;

        if (limitBreech)
            // Schedule a job that will call the event active if it runs.
            scheduleJob();
        else
            unscheduleJob(limitBreechInactiveTime);
    }

    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        double newDouble = newValue.getDoubleValue();

        // Add the value to the boxcar.
        boxcar.push(newDouble);

        // Trim the boxcar to the max size
        while (boxcar.size() > vo.getBoxcar())
            boxcar.pop();

        // Calculate the smoothness
        double smoothness = calc();

        if (smoothness < vo.getLimit()) {
            if (!limitBreech) {
                limitBreechActiveTime = newValue.getTime();
                changeLimitBreech();
            }
        }
        else {
            if (limitBreech) {
                limitBreechInactiveTime = newValue.getTime();
                changeLimitBreech();
            }
        }
    }

    @Override
    protected long getConditionActiveTime() {
        return limitBreechActiveTime;
    }

    /**
     * This method is only called when the event changes between being active or not, i.e. if the event currently is
     * active, then it should never be called with a value of true. That said, provision is made to ensure that the
     * limit is breeched before allowing the event to go active.
     * 
     * @param b
     */
    @Override
    synchronized public void setEventActive(boolean b) {
        eventActive = b;
        if (eventActive) {
            // Just for the fun of it, make sure that the limit is breeched.
            if (limitBreech)
                // Ok, things are good. Carry on...
                // Raise the event.
                raiseEvent(limitBreechActiveTime + getDurationMS(), createEventContext());
            else
                eventActive = false;
        }
        else
            // Deactive the event.
            returnToNormal(limitBreechInactiveTime);
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

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return "Smoothness Detector " + this.vo.getXid();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.rejectionHandler.rejectedHighPriorityTask(reason);
	}
}
