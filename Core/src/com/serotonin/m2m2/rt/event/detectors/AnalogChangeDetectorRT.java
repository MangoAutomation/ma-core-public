/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * TODO This class is a work in progress and IS NOT USABLE in its current state. 
 * 
 * The AnalogChangeDetector is used to detect occurrences of point values changing by a given amount within a duration of time.
 * 
 * For example a user may want to know if the water pressure drops suddenly but not be interested in a slowly dropping pressure.
 * 
 * This detector works by first determining if the user is tracking upward or downward changes.
 * 
 * Upward changes:
 * The detector tracks the lowest value during the period of interest.  For non-durational detectors this is the 
 * life of the point.  For durational detectors this is during the period.  
 * If the current value - lowest value > maxAllowedChange then the event will fire. 
 * 
 * 
 * With duration:
 * We need to track the start and end of the periods and ensure that the 'lowest' value is in the period,
 * discarding older values.  This can be only be done by maintaining a list of previous Point Values and dropping
 * them from the list over time. 
 * 
 * 
 * The configuration fields provided are static for the lifetime of this detector. The state fields vary based on the
 * changing conditions in the system. 
 * 
 * In particular, the highLimitActive field describes whether the point's value is
 * currently above the high limit or not. The eventActive field describes whether the point's value has been above the
 * high limit for longer than the tolerance duration.
 * 
 * @author Terry Packer
 */
public class AnalogChangeDetectorRT extends TimeoutDetectorRT<AnalogChangeDetectorVO> {

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single high limit exceed.
     */
    private boolean eventActive;
    
    /**
     * State field.  The total cumulative change
     * @param vo
     */
	private Double cumulativeChange;
	
    public AnalogChangeDetectorRT(AnalogChangeDetectorVO vo) {
        super(vo);
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.njbGetDataPoint().getName();
        String prettyLimit = vo.njbGetDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
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

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
    	
    	//Ensure we are ready to track changes, we may be at the start of an interval
    	
    	//Track the cumulative change, ( over a given time period if required )
    	this.cumulativeChange = this.cumulativeChange + (newValue.getDoubleValue() - oldValue.getDoubleValue());
    	
    	if(this.cumulativeChange > this.vo.getLimit()){
    		//Fire Event and 
    	}
    }
    
	/* 
	 * Call to check changes and see if we have changed enough
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.event.detectors.TimeoutDetectorRT#scheduleTimeoutImpl(long)
	 */
	@Override
	protected void scheduleTimeoutImpl(long fireTime) {
		
		
	}
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return "AnalogChange Detector " + this.vo.getXid();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.highPriorityRejectionHandler.rejected(reason);
	}

}
