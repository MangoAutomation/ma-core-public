/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;

/**
 * Detector that takes the average rate of change during a period 
 * and raises the event if the Roc is above or below the setpoint RoC
 * 
 * 
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorRT extends TimeoutDetectorRT<RateOfChangeDetectorVO> {

    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single range event.
     */
    private boolean eventActive;
    
    /**
     * Historical queue of values to track
     * TODO Probably copy on write array list or use a running RoC?
     */
    private List<PointValueTime> history;
    
    /**
     * The RoC per millisecond to compare to
     */
    private double comparisonRoCPerMs;
    
    public RateOfChangeDetectorRT(RateOfChangeDetectorVO vo) {
        super(vo);
        this.history = new ArrayList<>();
    }
 
    @Override
    public void initialize() {
        super.initialize();
        
        fillHistory();
        this.comparisonRoCPerMs = vo.getChange() / (double)getDurationMS();
        long now = Common.timer.currentTimeMillis();
        computeEventState(now);
        scheduleJob(now + getDurationMS());
    }


    @Override
    public void scheduleTimeoutImpl(long fireTime) {
        //Check if our RoC is outside of range
        computeEventState(fireTime);
        scheduleJob(fireTime + getDurationMS());
    }
    

    protected synchronized void setEventActive(long fireTime) {
        eventActive = true;
        raiseEvent(fireTime, createEventContext());
    }

    protected synchronized void setEventInactive(long timestamp) {
        eventActive = false;
        returnToNormal(timestamp);
    }

    @Override
    protected String getThreadNameImpl() {
        return "RateOfChange Detector " + vo.getXid();
    }

    @Override
    protected TranslatableMessage getMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }
    
    /**
     * These will always be in time order as this call won't happen on backdates
     */
    @Override
    synchronized public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        history.add(newValue);
    }
    
    private synchronized void computeEventState(long fireTime) {
        trimHistory(fireTime);
        if(history.size() >= 2){
            //TODO Use tolerance?
            double averageRoCPerMs = firstLastRocAlgorithm();
            if(compareRoC(averageRoCPerMs))
                setEventActive(fireTime);
            else
                setEventInactive(fireTime);
        }
    }
    
    private boolean compareRoC(double roc) {
        return roc > this.comparisonRoCPerMs;
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
        return now - getDurationMS();
    }
    
    /**
     * Compute the Rate of Change by using the first and last values over the full period 
     * @return
     */
    protected double firstLastRocAlgorithm(){
        PointValueTime oldest = history.get(0);
        PointValueTime latest = history.get(history.size() - 1);
        return (latest.getDoubleValue() - oldest.getDoubleValue())/(double)getDurationMS();
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
