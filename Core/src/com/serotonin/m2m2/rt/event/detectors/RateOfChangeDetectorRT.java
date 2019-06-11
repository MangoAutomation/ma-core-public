/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.CalculationMode;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.ComparisonMode;

/**
 * 
 * RoC = (latestValue - periodStartValue)/periodDurationMs
 * 
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
     * Time when the RoC started to change, raise events using this offset
     */
    private long periodStartTime;
    
    /**
     * Oldest value to use in RoC computation
     */
    private Double periodStartValue;
    
    /**
     * Latest value to use in RoC computation
     */
    private PointValueTime latestValue;
    
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
     * How often to check the RoC when no values are coming in
     */
    private long rocCheckPeriodMs;
    
    /**
     * State field. Whether the RoC event is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single RoC threshold exceeded event.
     */
    private boolean rocBreachActive;

    private long rocBreachActiveTime;
    private long rocBreachInactiveTime;
    
    /**
     * So we have access to our point's latest value
     */
    private DataPointRT rt;
    
    /**
     * Task used to see if we have dropped below our threshold due to
     * no changes be recorded.
     */
    private TimeoutClient rocTimeoutClient;
    private TimeoutTask rocTimeoutTask;
    
    public RateOfChangeDetectorRT(RateOfChangeDetectorVO vo) {
        super(vo);
        this.rocTimeoutClient = new TimeoutClient() {

            @Override
            public void scheduleTimeout(long fireTime) {
                //Slide the window
                if(fireTime >= periodStartTime + rocDurationMs) {
                    if(latestValue != null)
                        periodStartValue = latestValue.getDoubleValue();
                    periodStartTime = computePeriodStart(fireTime);
                    PointValueTime pvt = rt.getPointValue();
                    if(pvt != null)
                        latestValue = new PointValueTime(rt.getPointValue().getDoubleValue(), fireTime);
                    else
                        latestValue = null;
                }
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
        long time = Common.timer.currentTimeMillis();
        rt = Common.runtimeManager.getDataPoint(vo.getDataPoint().getId());
        
        if(vo.getCalculationMode() == CalculationMode.AVERAGE) {
            comparisonRoCPerMs = vo.getRateOfChangeThreshold() / (double)Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), vo.getRateOfChangeThresholdPeriods());
            rocDurationMs = Common.getMillis(vo.getRateOfChangePeriodType(), vo.getRateOfChangePeriods());
            if(vo.isUseResetThreshold())
                resetRoCPerMs = vo.getResetThreshold() / (double)Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), vo.getRateOfChangeThresholdPeriods());

            DataSourceVO<?> ds = DataSourceDao.getInstance().get(rt.getDataSourceId());
            if(ds instanceof PollingDataSourceVO) {
                //Use poll period
                rocCheckPeriodMs = Common.getMillis(((PollingDataSourceVO<?>)ds).getUpdatePeriodType(), ((PollingDataSourceVO<?>)ds).getUpdatePeriods());
            }else {
                //Use factor of rocDurationMs
                rocCheckPeriodMs = rocDurationMs/10;
                if(rocCheckPeriodMs == 0)
                    rocCheckPeriodMs = 5;
            }
        }else {
            comparisonRoCPerMs = vo.getRateOfChangeThreshold() / (double)Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), vo.getRateOfChangeThresholdPeriods());;
            if(vo.isUseResetThreshold())
                resetRoCPerMs = vo.getResetThreshold() / (double)Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), vo.getRateOfChangeThresholdPeriods());;
        }
        
        //Fill our  values
        latestValue = rt.getPointValue();
        periodStartTime = computePeriodStart(time);
        
        //Do we have a value exactly at the period start?
        PointValueTime start = rt.getPointValueAt(periodStartTime);
        if(start == null)
            start = rt.getPointValueBefore(periodStartTime);
        
        //Do we have a value to compute the RoC?
        if(start != null) {
            periodStartValue = start.getDoubleValue();
            double currentRoc = firstLastRocAlgorithm();
            checkState(currentRoc, time, latestValue != null ? latestValue.getTime() : time);
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
    
    private void changeHighLimitActive() {
        rocBreachActive = !rocBreachActive;
        if (rocBreachActive)
            // Schedule a job that will call the event active if it runs.
            scheduleJob(rocBreachActiveTime);
        else
            unscheduleJob(rocBreachInactiveTime);
    }
    
    /**
     * These will always be in time order as this call won't happen on backdates
     */
    @Override
    synchronized public void pointUpdated(PointValueTime newValue) {
        //Is our new value in a new period?
        long now = Common.timer.currentTimeMillis();
        if(newValue.getTime() >= periodStartTime + rocDurationMs) {
            if(latestValue == null) {
                //First value ever
                periodStartValue = newValue.getDoubleValue();
                if(vo.getCalculationMode() == CalculationMode.INSTANTANEOUS)
                    periodStartTime = newValue.getTime();
            }else {
                periodStartValue = latestValue.getDoubleValue();
                if(vo.getCalculationMode() == CalculationMode.INSTANTANEOUS)
                    periodStartTime = latestValue.getTime();
            }
            
            if(vo.getCalculationMode() == CalculationMode.AVERAGE)
                periodStartTime = computePeriodStart(now);
            
            latestValue = newValue;
        }else {
            latestValue = newValue;   
        }
        
        rocChanged(now);
    }
    
    private synchronized void rocChanged(long now) {
        if(latestValue != null) {
            double currentRoc = firstLastRocAlgorithm();
            checkState(currentRoc, now, latestValue.getTime());
        }else {
            //Assume 0 RoC
            checkState(0.0d, now, periodStartTime);
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
                    changeHighLimitActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    if ((rocBreachActive)&&(currentRoc >= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }else{
                    //Not using reset
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }
            }
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN){
            if (currentRoc > comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeHighLimitActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    //Turn off alarm if we are active and below the Reset value
                    if ((rocBreachActive) &&(currentRoc <= resetRoCPerMs)){
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }else{
                    //Turn off alarm if we are active
                    if (rocBreachActive){
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }
            }
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN_OR_EQUALS){
            //Not Lower than
            if (currentRoc >= comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeHighLimitActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    if ((rocBreachActive) && (currentRoc <= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }else{
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }
            }
        }else{ //mode must be LESS_THAN
            //is lower than
            if (currentRoc < comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeHighLimitActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    if ((rocBreachActive) && (currentRoc >= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }else{
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeHighLimitActive();
                    }
                }
            }
        }
        //Schedule our timeout task to check our RoC next period as if no values show up 
        // old ones may slide out of our window and change our RoC
        if(vo.getCalculationMode() == CalculationMode.AVERAGE)
            rocTimeoutTask = new TimeoutTask(new Date(fireTime + rocCheckPeriodMs), rocTimeoutClient);
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
    
    private long computePeriodStart(long now){
        return now - rocDurationMs;
    }
    
    /**
     * Compute the Rate of Change by using the first and last values over the full period 
     * @return
     */
    protected double firstLastRocAlgorithm(){
        if(vo.getCalculationMode() == CalculationMode.AVERAGE)
            return (latestValue.getDoubleValue() - periodStartValue)/(double)rocDurationMs;
        else {
            long duration = latestValue.getTime() - periodStartTime;
            if(duration > 0)
                return (latestValue.getDoubleValue() - periodStartValue)/(double)duration;
            else
                return 0; //TODO what do we do here?
        }
    }
    
    @Override
    public void terminate() {
        cancelRocTimeoutTask();
        super.terminate();
    }
}
