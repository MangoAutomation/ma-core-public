/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;

import edu.emory.mathcs.backport.java.util.Collections;

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
    
    private double max = -Double.MAX_VALUE;
    private double min = Double.MAX_VALUE;
    private final long durationMillis;
    
    private final List<PointValueTime> periodValues;
	
    public AnalogChangeDetectorRT(AnalogChangeDetectorVO vo) {
        super(vo);
        this.durationMillis = Common.getMillis(vo.getDurationType(), vo.getDuration());
        PointValueDao pvd = Common.databaseProxy.newPointValueDao();
        long now = Common.timer.currentTimeMillis();
        periodValues = new ArrayList<>();
        PointValueTime periodStartValue = pvd.getPointValueBefore(vo.getSourceId(), now - durationMillis + 1);
        if(periodStartValue != null)
            periodValues.add(periodStartValue);
        periodValues.addAll(pvd.getPointValues(vo.getSourceId(), now - durationMillis + 1));
        
        Iterator<PointValueTime> iter = periodValues.iterator();
        while(iter.hasNext()) {
            PointValueTime pvt = iter.next();
            if(pvt.getDoubleValue() > max)
                max = pvt.getDoubleValue();
            if(pvt.getDoubleValue() < min)
                min = pvt.getDoubleValue();
        }
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.njbGetDataPoint().getExtendedName();
        String prettyLimit = vo.njbGetDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();
        
        if(vo.isCheckIncrease() && vo.isCheckDecrease())
            return new TranslatableMessage("event.detector.analogChangePeriod", name, prettyLimit, durationDescription);
        else if(vo.isCheckIncrease())
            return new TranslatableMessage("event.detector.analogIncreasePeriod", name, prettyLimit, durationDescription);
        else if(vo.isCheckDecrease())
            return new TranslatableMessage("event.detector.analogDecreasePeriod", name, prettyLimit, durationDescription);
        else
            throw new ShouldNeverHappenException("Illegal state for analog change detector" + vo.getXid());
    }

    @Override
	public boolean isEventActive() {
        return eventActive;
    }
    
    private void pruneValueList(long time) {
        long cutoff = time - durationMillis;
        boolean recomputeMinimum = false, recomputeMaximum = false;
//        while(iter.hasNext()) {
//            PointValueTime pvt = iter.next();
//            if(pvt.getTime() < cutoff) {
//                if(pvt.getDoubleValue() >= max) {
//                    recomputeMaximum = true;
//                }
//                if(pvt.getDoubleValue() <= min)
//                    recomputeMinimum = true;
//                iter.remove();
//            }
//        }
        
        Collections.sort(periodValues);
        while(periodValues.size() > 1) {
            PointValueTime pvt1 = periodValues.get(0);
            PointValueTime pvt2 = periodValues.get(1);
            if(pvt2.getTime() <= cutoff) {
                if(pvt1.getDoubleValue() >= max)
                    recomputeMaximum = true;
                if(pvt1.getDoubleValue() <= min)
                    recomputeMinimum = true;
                periodValues.remove(0);
            } else {
                break;
            }
        }
        
        recomputeMaximum |= periodValues.size() <= 1;
        recomputeMinimum |= periodValues.size() <= 1;
        
        if(recomputeMaximum || recomputeMinimum) {
            double newMax = -Double.MAX_VALUE;
            double newMin = Double.MAX_VALUE;
            Iterator<PointValueTime> iter = periodValues.iterator();
            while(iter.hasNext()) {
                PointValueTime pvt = iter.next();
                if(pvt.getDoubleValue() > newMax)
                    newMax = pvt.getDoubleValue();
                if(pvt.getDoubleValue() < newMin)
                    newMin = pvt.getDoubleValue();
            }
            if(recomputeMaximum)
                max = newMax;
            if(recomputeMinimum)
                min = newMin;
        }
    }
    
    private boolean checkNewValue(PointValueTime newValue) {
        boolean active = false;
        if(periodValues.size() > 0) {
            if(vo.isCheckIncrease() && newValue.getDoubleValue() > min + vo.getLimit()) {
                active = true;
            }
            if(vo.isCheckDecrease() && newValue.getDoubleValue() < max - vo.getLimit()) {
                active = true;
            }
        }
        
        periodValues.add(newValue);
        if(newValue.getDoubleValue() > max)
            max = newValue.getDoubleValue();
        if(newValue.getDoubleValue() < min)
            min = newValue.getDoubleValue();
        return active;
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        boolean raised = false;
        synchronized(periodValues) {
        	unscheduleJob();
        	pruneValueList(newValue.getTime());
        	raised = checkNewValue(newValue);
        	scheduleJob(Common.timer.currentTimeMillis() + durationMillis);
        }
        
        if(raised && !eventActive) {
            raiseEvent(newValue.getTime(), null);
            eventActive = true;
        } else if(!raised && eventActive) {
            returnToNormal(newValue.getTime());
            eventActive = false;
        }
    }
    
	/* 
	 * Call to check changes and see if we have changed enough
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.event.detectors.TimeoutDetectorRT#scheduleTimeoutImpl(long)
	 */
	@Override
	protected void scheduleTimeoutImpl(long fireTime) {
		synchronized(periodValues) {
		    periodValues.clear();
		    max = -Double.MAX_VALUE;
		    min = Double.MAX_VALUE;
		    returnToNormal(fireTime);
		    eventActive = false;
		}
	}
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadNameImpl() {
		return "AnalogChange Detector " + this.vo.getXid();
	}

}
