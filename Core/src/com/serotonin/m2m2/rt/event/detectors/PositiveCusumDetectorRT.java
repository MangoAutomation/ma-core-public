/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.PositiveCusumDetectorVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * The PositiveCusumDetector is used to detect occurrences of point values exceeding the given CUSUM limit for a given
 * duration. For example, a user may need to have an event raised when a temperature CUSUM exceeds some value for 10
 * minutes or more.
 * 
 * @author Matthew Lohbihler
 */
public class PositiveCusumDetectorRT extends TimeDelayedEventDetectorRT<PositiveCusumDetectorVO> {
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

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.njbGetDataPoint().getName();
        String prettyLimit = vo.njbGetDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
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
    private void changePositiveCusumActive() {
        positiveCusumActive = !positiveCusumActive;

        if (positiveCusumActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob();
        else
            unscheduleJob(positiveCusumInactiveTime);
    }

    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        double newDouble = newValue.getDoubleValue();

        cusum += newDouble - vo.getWeight();
        if (cusum < 0)
            cusum = 0;

        if (cusum > vo.getLimit()) {
            if (!positiveCusumActive) {
                positiveCusumActiveTime = newValue.getTime();
                changePositiveCusumActive();
            }
        }
        else {
            if (positiveCusumActive) {
                positiveCusumInactiveTime = newValue.getTime();
                changePositiveCusumActive();
            }
        }
    }

    @Override
    protected long getConditionActiveTime() {
        return positiveCusumActiveTime;
    }

    /**
     * This method is only called when the event changes between being active or not, i.e. if the event currently is
     * active, then it should never be called with a value of true. That said, provision is made to ensure that the
     * postive CUSUM is active before allowing the event to go active.
     * 
     * @param b
     */
    @Override
    synchronized public void setEventActive(boolean b) {
        eventActive = b;
        if (eventActive) {
            // Just for the fun of it, make sure that the positive CUSUM is active.
            if (positiveCusumActive)
                // Ok, things are good. Carry on...
                // Raise the event.
                raiseEvent(positiveCusumActiveTime + getDurationMS(), createEventContext());
            else
                eventActive = false;
        }
        else
            // Deactive the event.
            returnToNormal(positiveCusumInactiveTime);
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return "PosCusumDetector " + this.vo.getXid();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.rejectionHandler.rejectedHighPriorityTask(reason);
	}
}
