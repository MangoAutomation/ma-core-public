/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event.detectors;

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

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.njbGetDataPoint().getExtendedName();
        String prettyHighRange = vo.njbGetDataPoint().getTextRenderer().getText(vo.getHigh(), TextRenderer.HINT_SPECIFIC);
        String prettyLowRange = vo.njbGetDataPoint().getTextRenderer().getText(vo.getLow(), TextRenderer.HINT_SPECIFIC);
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
     * 
     * @param b
     */
    private void changeRangeActive() {
    	rangeActive = !rangeActive;

        if (rangeActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob();
        else
            unscheduleJob(rangeInactiveTime);
    }

    @Override
    synchronized public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        double newDouble = newValue.getDoubleValue();
        
        //Are we supposed to be within or outside?
        if(vo.isWithinRange()){
        	//Fire event if within range
	        if ((newDouble <= vo.getHigh())&&(newDouble >= vo.getLow())) {
	            if (!rangeActive) {
	            	rangeActiveTime = newValue.getTime();
	                changeRangeActive();
	            }
	        }
	        else {
	            if (rangeActive) {
	            	rangeInactiveTime = newValue.getTime();
	                changeRangeActive();
	            }
	        }
        }else{
        	//Fire event if Outside Range detection
	        if ((newDouble >= vo.getHigh())||(newDouble <= vo.getLow())) {
	            if (!rangeActive) {
	            	rangeActiveTime = newValue.getTime();
	                changeRangeActive();
	            }
	        }
	        else {
	            if (rangeActive) {
	            	rangeInactiveTime = newValue.getTime();
	                changeRangeActive();
	            }
	        }
        }
    }

    @Override
    protected long getConditionActiveTime() {
        return rangeActiveTime;
    }

    /**
     * This method is only called when the event changes between being active or not, i.e. if the event currently is
     * active, then it should never be called with a value of true. That said, provision is made to ensure that the high
     * limit is active before allowing the event to go active.
     * 
     * @param b
     */
    @Override
    synchronized public void setEventActive(boolean b) {
        eventActive = b;
        if (eventActive) {
            // Just for the fun of it, make sure that the high limit is active.
            if (rangeActive)
                // Ok, things are good. Carry on...
                // Raise the event.
                raiseEvent(rangeActiveTime + getDurationMS(), createEventContext());
            else
                eventActive = false;
        }
        else
            // Deactive the event.
            returnToNormal(rangeInactiveTime);
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadNameImpl() {
		return "AnalogLowLimit Detector " + this.vo.getXid();
	}

}
