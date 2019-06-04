/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.ComparisonMode;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorRT extends TimeDelayedEventDetectorRT<RateOfChangeDetectorVO> {

private final Log log = LogFactory.getLog(RateOfChangeDetectorRT.class);
    
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
     * State field. Whether the RoC event is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single RoC threshold exceeded event.
     */
    private boolean rocBreachActive;

    private long rocBreachActiveTime;
    private long rocBreachInactiveTime;
    
    /**
     * Task used to see if we have dropped below our threshold due to
     * no changes be recorded.
     */
    private TimeoutClient rocTimeoutClient;
    private TimeoutTask rocTimeoutTask;
    
    public RateOfChangeDetectorRT(RateOfChangeDetectorVO vo) {
        super(vo);
        this.history = new ArrayList<>();
        this.rocTimeoutClient = new TimeoutClient() {

            @Override
            public void scheduleTimeout(long fireTime) {
                rocChanged(fireTime);
            }

            @Override
            public String getThreadName() {
                return "ROCD" + vo.getXid();
            }
        };
    }
 
    @Override
    public void initializeState() {
        this.comparisonRoCPerMs = vo.getRateOfChangeThreshold() / (double)Common.getMillis(vo.getRateOfChangePeriodType(), vo.getRateOfChangePeriods());
        if(vo.getResetThreshold() != null)
            this.resetRoCPerMs = vo.getResetThreshold() / (double)Common.getMillis(vo.getRateOfChangePeriodType(), vo.getRateOfChangePeriods());
        this.rocDurationMs = Common.getMillis(vo.getRateOfChangePeriodType(), vo.getRateOfChangePeriods());

        long time = Common.timer.currentTimeMillis();
        fillHistory(time);
        
        if(this.history.size() >=2) {
            double currentRoc = firstLastRocAlgorithm();
            checkState(currentRoc, time, history.get(history.size() - 1).getTime());
        }else if(this.history.size() == 1) {
            //Assume 0 RoC
            checkState(0.0d, time, history.get(0).getTime());
        }
    }


    @Override
    protected String getThreadNameImpl() {
        return "HighLimitRateOfChangeDetector " + vo.getXid();
    }

    @Override
    protected TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyChange = vo.getDataPoint().getTextRenderer().getText(vo.getRateOfChangeThreshold(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();
        TranslatableMessage rocDurationDescription = vo.getRateOfChangeDurationDescription();
        
        if(vo.getComparisonMode() == ComparisonMode.LESS_THAN_OR_EQUALS){
            //Not Higher than 
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.highLimitRateOfChangeNotHigher", name, prettyChange, rocDurationDescription);
            return new TranslatableMessage("event.detector.highLimitRateOfChangeNotHigherPeriod", name, prettyChange, rocDurationDescription, durationDescription);
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN){
            //Higher than
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.highLimitRateOfChange", name, prettyChange, rocDurationDescription);
            return new TranslatableMessage("event.detector.highLimitRateofChangePeriod", name, prettyChange, rocDurationDescription, durationDescription);
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN_OR_EQUALS){
            //Not Higher than 
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.lowLimitRateOfChangeNotLower", name, prettyChange, rocDurationDescription);
            return new TranslatableMessage("event.detector.lowLimitRateofChangeNotLowerPeriod", name, prettyChange, rocDurationDescription, durationDescription);
        }else{
            //Higher than
            if (durationDescription == null)
                return new TranslatableMessage("event.detector.lowLimitRateOfChange", name, prettyChange, rocDurationDescription);
            return new TranslatableMessage("event.detector.lowLimitRateOfChangePeriod", name, prettyChange, rocDurationDescription, durationDescription);
        }
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }
    
    private void changeHighLimitActive(long time) {
        rocBreachActive = !rocBreachActive;
        if (rocBreachActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(time);
        else
            unscheduleJob(rocBreachInactiveTime);
    }
    
    /**
     * These will always be in time order as this call won't happen on backdates
     */
    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        history.add(newValue);
        long time = Common.timer.currentTimeMillis();
        rocChanged(time);
    }
    
    private synchronized void rocChanged(long now) {
        trimHistory(now);
        if(this.history.size() >=2) {
            double currentRoc = firstLastRocAlgorithm();
            checkState(currentRoc, now, history.get(history.size() - 1).getTime());
        }else if(this.history.size() == 1) {
            //Assume 0 RoC
            checkState(0.0d, now, history.get(0).getTime());
        }
    }
    
    private synchronized void checkState(double currentRoc, long fireTime, long latestValueTime) {
        cancelRocTimeoutTask();
        
        if(vo.isUseAbsoluteValue())
            currentRoc = Math.abs(currentRoc);
        
        if(vo.getComparisonMode() == ComparisonMode.LESS_THAN_OR_EQUALS){
            if (currentRoc <= comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeHighLimitActive(fireTime);
                }
            }
            else {
                //Are we using a reset value
                if(vo.getResetThreshold() != null){
                    if ((rocBreachActive)&&(currentRoc >= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }else{
                    //Not using reset
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }
            }
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN){
            if (currentRoc > comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeHighLimitActive(fireTime);
                }
            }
            else {
                //Are we using a reset value
                if(vo.getResetThreshold() != null){
                    //Turn off alarm if we are active and below the Reset value
                    if ((rocBreachActive) &&(currentRoc <= resetRoCPerMs)){
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }else{
                    //Turn off alarm if we are active
                    if (rocBreachActive){
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }
            }
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN_OR_EQUALS){
            //Not Lower than
            if (currentRoc >= comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeHighLimitActive(fireTime);
                }
            }
            else {
                //Are we using a reset value
                if(vo.getResetThreshold() != null){
                    if ((rocBreachActive) && (currentRoc <= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }else{
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }
            }
        }else{ //mode must be LESS_THAN
            //is lower than
            if (currentRoc < comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeHighLimitActive(fireTime);
                }
            }
            else {
                //Are we using a reset value
                if(vo.getResetThreshold() != null){
                    if ((rocBreachActive) && (currentRoc >= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }else{
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive(fireTime);
                    }
                }
            }
        }
        //Schedule our timeout task to check our RoC next period if we are not active
        if(!this.eventActive)
            scheduleRocTimeoutTask(fireTime + rocDurationMs);
    }
    
    private void scheduleRocTimeoutTask(long nextCheck) {
        if(rocTimeoutTask != null)
            cancelRocTimeoutTask();
        rocTimeoutTask = new TimeoutTask(new Date(nextCheck), rocTimeoutClient);
    }
    
    synchronized private void cancelRocTimeoutTask() {
        if (rocTimeoutTask != null) {
            rocTimeoutTask.cancel();
            rocTimeoutTask = null;
        }
    }
    
    @Override
    protected long getConditionActiveTime() {
        return rocBreachActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(rocBreachInactiveTime);
    }
    
    @Override
    protected void setEventActive(long timestamp) {
        this.eventActive = true;
        // Just for the fun of it, make sure that the high limit is active.
        if (rocBreachActive)
            raiseEvent(rocBreachActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when roc detector is not active. Ignoring.");
            eventActive = false;
        }
    }
    
    private void trimHistory(long now) {
        if(this.history.size() == 0)
            return;
        long since = computePeriodStart(now);
        this.history = this.history.stream().filter(pvt -> pvt.getTime() >= since).collect(Collectors.toList());
    }

    private void fillHistory(long now) {
        history.clear();

        DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getDataPoint().getId());
        if(rt == null)
            return;
        
        long since = computePeriodStart(now);
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
