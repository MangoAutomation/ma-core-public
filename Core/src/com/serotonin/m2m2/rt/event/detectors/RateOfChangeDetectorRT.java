/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;
import com.serotonin.util.queue.ObjectQueue;

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
    private ObjectQueue<PointValueTime> history;
    
    public RateOfChangeDetectorRT(RateOfChangeDetectorVO vo) {
        super(vo);
        this.history = new ObjectQueue<>();
    }
 
    @Override
    public void initialize() {
        super.initialize();
        
        fillHistory();

        long now = Common.timer.currentTimeMillis();
        computeEventState(now);

        if (getDurationMS() > 0)
            scheduleJob(now + getDurationMS());
    }


    @Override
    public void scheduleTimeoutImpl(long fireTime) {
        //Check if our RoC is outside of range
        computeEventState(fireTime);
        if (getDurationMS() > 0)
            scheduleJob(fireTime + getDurationMS());
    }
    

    protected synchronized void setEventActive(long fireTime) {
        eventActive = true;
        raiseEvent(fireTime, createEventContext());
    }

    protected void setEventInactive(long timestamp) {
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
    
    @Override
    synchronized public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        trimHistory();
        history.push(newValue);
        if (getDurationMS() <= 0)
            computeEventState(Common.timer.currentTimeMillis());
    }
    
    private void computeEventState(long fireTime) {
        if(history.size() >= 2){
            //TODO Use tolerance
            double averageRoCPerMs = halfPeriodRocAlgorithm();
            if(averageRoCPerMs > vo.getChange())
                setEventActive(fireTime);
            else
                setEventInactive(fireTime);
            
        }else if(history.size() == 1){
            //Simply compare to history[0]
            if(history.peek(0).getDoubleValue() > vo.getChange())
                setEventActive(fireTime);
            else
                setEventInactive(fireTime);
        }
        //TODO Else ensure event inactive?
       
    }
    
    private void trimHistory() {
        if(this.history.size() == 0)
            return;
        
        long since = computePeriodStart();
        
        //TODO Could be cleaned up a little
        PointValueTime pvt = this.history.peek(0);
        while((pvt != null)&&(pvt.getTime() < since)){
            if(history.size() > 0)
                pvt = history.pop();
            else
                pvt = null;
        }
    }

    private void fillHistory() {
        history.clear();

        DataPointRT rt = Common.runtimeManager.getDataPoint(vo.getDataPoint().getId());
        if(rt == null)
            return;
        
        long since = computePeriodStart();
        
        List<PointValueTime> pvts = rt.getPointValues(since);
        for(PointValueTime pvt : pvts)
            history.push(pvt);
        
    }
    
    private long computePeriodStart(){
        return Common.timer.currentTimeMillis() - getDurationMS();
    }
    
    /**
     * Compute the Rate Of Change by using the average of the 
     * first and last halves of the period as the points on a line
     * @return
     */
    protected double halfPeriodRocAlgorithm(){
        
        int midpoint = history.size()/2;
        double firstAvg=0d,lastAvg=0d;
        
        for(int i=0; i<midpoint; i++){
            firstAvg += history.peek(i).getDoubleValue();
        }
        firstAvg = firstAvg / (double)(midpoint);
        
        for(int i=midpoint; i<history.size(); i++){
            lastAvg += history.peek(i).getDoubleValue();
        }
        lastAvg = lastAvg / (double)(history.size() - midpoint);
        
        
        PointValueTime oldest = history.peek(0);
        PointValueTime latest = history.peek(history.size() - 1);
        
        return (lastAvg - firstAvg)/(double)(latest.getTime() - oldest.getTime());
    }
    
}
