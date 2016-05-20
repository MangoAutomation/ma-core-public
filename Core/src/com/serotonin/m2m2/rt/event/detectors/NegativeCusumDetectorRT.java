/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.NegativeCusumDetectorVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * The NegativeCusumDetectorRT is used to detect occurances of point values below the given CUSUM limit for a given
 * duration. For example, a user may need to have an event raised when a temperature CUSUM sinks below some value for 10
 * minutes or more.
 * 
 * @author Matthew Lohbihler
 */
public class NegativeCusumDetectorRT extends TimeDelayedEventDetectorRT<NegativeCusumDetectorVO> {
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

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.njbGetDataPoint().getExtendedName();
        String prettyLimit = vo.njbGetDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
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
    private void changeNegativeCusumActive() {
        negativeCusumActive = !negativeCusumActive;

        if (negativeCusumActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob();
        else
            unscheduleJob(negativeCusumInactiveTime);
    }

    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        double newDouble = newValue.getDoubleValue();

        cusum += newDouble - vo.getWeight();
        if (cusum > 0)
            cusum = 0;

        if (cusum < vo.getLimit()) {
            if (!negativeCusumActive) {
                negativeCusumActiveTime = newValue.getTime();
                changeNegativeCusumActive();
            }
        }
        else {
            if (negativeCusumActive) {
                negativeCusumInactiveTime = newValue.getTime();
                changeNegativeCusumActive();
            }
        }
    }

    @Override
    protected long getConditionActiveTime() {
        return negativeCusumActiveTime;
    }

    /**
     * This method is only called when the event changes between being active or not, i.e. if the event currently is
     * active, then it should never be called with a value of true. That said, provision is made to ensure that the
     * negative CUSUM limit is active before allowing the event to go active.
     * 
     * @param b
     */
    @Override
    synchronized public void setEventActive(boolean b) {
        eventActive = b;
        if (eventActive) {
            // Just for the fun of it, make sure that the negative CUSUM is active.
            if (negativeCusumActive)
                // Ok, things are good. Carry on...
                // Raise the event.
                raiseEvent(negativeCusumActiveTime + getDurationMS(), createEventContext());
            else
                eventActive = false;
        }
        else
            // Deactive the event.
            returnToNormal(negativeCusumInactiveTime);
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return "NegativeCusum Detector " + this.vo.getXid();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.rejectionHandler.rejectedHighPriorityTask(reason);
	}
}
