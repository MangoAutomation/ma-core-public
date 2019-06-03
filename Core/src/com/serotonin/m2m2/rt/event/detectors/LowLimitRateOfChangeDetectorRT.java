/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.LowLimitRateOfChangeDetectorVO;

/**
 * Detector that takes the average rate of change during a period 
 * and raises the event if the Roc is above or below the setpoint RoC
 * 
 * 
 * @author Terry Packer
 *
 */
public class LowLimitRateOfChangeDetectorRT extends TimeDelayedEventDetectorRT<LowLimitRateOfChangeDetectorVO> {
    private final Log log = LogFactory.getLog(LowLimitRateOfChangeDetectorRT.class);
    
    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single range event.
     */
    private boolean eventActive;
    
    /**
     * Historical queue of values to track
     */
    private List<PointValueTime> history;
    
    /**
     * The RoC per millisecond to compare to
     */
    private double comparisonRoCPerMs;
    
    /**
     * The RoC per millisecond to reset on
     */
    private double resetRoCPerMs;
    
    /**
     * The duration for the roc computation
     */
    private long rocDurationMs;
    
    /**
     * State field. Whether the low limit is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single low limit event.
     */
    private boolean lowLimitActive;

    private long lowLimitActiveTime;
    private long lowLimitInactiveTime;
    
    public LowLimitRateOfChangeDetectorRT(LowLimitRateOfChangeDetectorVO vo) {
        super(vo);
        this.history = new ArrayList<>();
    }
 
    @Override
    public void initialize() {
        super.initialize();
        fillHistory();
        this.comparisonRoCPerMs = vo.getChange() / (double)Common.getMillis(vo.getRocDurationType(), vo.getRocDuration());
        this.resetRoCPerMs = vo.getResetChange() / (double)Common.getMillis(vo.getRocDurationType(), vo.getRocDuration());
        this.rocDurationMs = Common.getMillis(vo.getRocDurationType(), vo.getRocDuration());
    }


    @Override
    protected String getThreadNameImpl() {
        return "LowLimitRateOfChangeDetector " + vo.getXid();
    }

    @Override
    protected TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyChange = vo.getDataPoint().getTextRenderer().getText(vo.getChange(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();
        
        if(vo.isNotLower()){
            //Not Higher than 
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.lowLimitRateOfChangeNotLower", name, prettyChange);
            return new TranslatableMessage("event.detector.lowLimitRateofChangeNotLowerPeriod", name, prettyChange, durationDescription);
        }else{
            //Higher than
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.lowLimitRateOfChange", name, prettyChange);
            return new TranslatableMessage("event.detector.lowLimitRateOfChangePeriod", name, prettyChange, durationDescription);
        }
 
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }
    
    private void changeLowLimitActive(long time) {
        lowLimitActive = !lowLimitActive;

        if (lowLimitActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(lowLimitInactiveTime);
    }
    
    /**
     * These will always be in time order as this call won't happen on backdates
     */
    @Override
    synchronized public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        history.add(newValue);
        
        long time = Common.timer.currentTimeMillis();
        trimHistory(time);
        if(this.history.size() >=2) {
            double newDouble = firstLastRocAlgorithm();
            
            if(vo.isNotLower()){
                //Not Lower than
                if (newDouble >= comparisonRoCPerMs) {
                    if (!lowLimitActive) {
                        lowLimitActiveTime = newValue.getTime();
                        changeLowLimitActive(time);
                    }
                }
                else {
                    //Are we using a reset value
                    if(vo.isUseResetChange()){
                        if ((lowLimitActive)&&(newDouble <= resetRoCPerMs)) {
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
                if (newDouble < comparisonRoCPerMs) {
                    if (!lowLimitActive) {
                        lowLimitActiveTime = newValue.getTime();
                        changeLowLimitActive(time);
                    }
                }
                else {
                    //Are we using a reset value
                    if(vo.isUseResetChange()){
                        if ((lowLimitActive)&&(newDouble >= resetRoCPerMs)) {
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
    }
    
    @Override
    protected long getConditionActiveTime() {
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
    
    private void trimHistory(long now) {
        if(this.history.size() == 0)
            return;
        long since = computePeriodStart(now);
        this.history = this.history.stream().filter(pvt -> pvt.getTime() >= since).collect(Collectors.toList());
    }

    private void fillHistory() {
        history.clear();

        DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getDataPoint().getId());
        if(rt == null)
            return;
        
        long since = computePeriodStart(Common.timer.currentTimeMillis());
        List<PointValueTime> pvts = rt.getPointValues(since);
        for(PointValueTime pvt : pvts)
            history.add(pvt);
        
    }
    
    private long computePeriodStart(long now){
        return now - rocDurationMs;
    }
    
    /**
     * Compute the Rate of Change by using the first and last values over the full period 
     * @return
     */
    protected double firstLastRocAlgorithm(){
        PointValueTime oldest = history.get(0);
        PointValueTime latest = history.get(history.size() - 1);
        return (latest.getDoubleValue() - oldest.getDoubleValue())/(double)rocDurationMs;
    }
    
    /**
     * Compute the Rate Of Change by using the first and last values
     *  in the period as points on a line
     * @return
     */
    protected double firstLastValuesRocAlgorithm(){
        PointValueTime oldest = history.get(0);
        PointValueTime latest = history.get(history.size() - 1);
        return (latest.getDoubleValue() - oldest.getDoubleValue())/(double)(latest.getTime() - oldest.getTime());
    }
    
    /**
     * Compute the Rate Of Change by using the average of the 
     * first and last halves of the period as the points on a line
     * @return
     */
    protected double halfPeriodRocValuesAlgorithm(){
        
        int midpoint = history.size()/2;
        double firstAvg=0d,lastAvg=0d;
        
        for(int i=0; i<midpoint; i++){
            firstAvg += history.get(i).getDoubleValue();
        }
        firstAvg = firstAvg / (double)(midpoint);
        
        for(int i=midpoint; i<history.size(); i++){
            lastAvg += history.get(i).getDoubleValue();
        }
        lastAvg = lastAvg / (double)(history.size() - midpoint);
        
        
        PointValueTime oldest = history.get(0);
        PointValueTime latest = history.get(history.size() - 1);
        
        return (lastAvg - firstAvg)/(double)(latest.getTime() - oldest.getTime());
    }
    
}
