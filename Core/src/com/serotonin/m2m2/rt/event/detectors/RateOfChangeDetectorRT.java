/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.CalculationMode;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.ComparisonMode;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.SimulationTimer;
import com.serotonin.timer.TimerTask;

/**
 *
 * RoC = (latestValue - periodStartValue)/periodDurationMs
 *
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorRT extends TimeDelayedEventDetectorRT<RateOfChangeDetectorVO> {

    private final Logger log = LoggerFactory.getLogger(RateOfChangeDetectorRT.class);

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
     * State field. Whether the RoC event is currently active or not. This field is used to prevent multiple events
     * being raised during the duration of a single RoC threshold exceeded event.
     */
    private boolean rocBreachActive;

    private long rocBreachActiveTime;
    private long rocBreachInactiveTime;

    /**
     * So we have access to our point's value at a given time
     */
    private Function<Long, PointValueTime> currentValueFunction;
    private DataPointRT rt;

    //For display in REST api
    private double latestRoc;

    /**
     * Tasks used to see if we have dropped below our threshold due to
     * no changes be recorded.
     */
    private final Set<RocTimeoutTask> rocTimeoutTasks;

    public RateOfChangeDetectorRT(RateOfChangeDetectorVO vo) {
        super(vo);
        this.rocTimeoutTasks = new HashSet<>();
    }

    public long getPeriodStartTime() {
        return periodStartTime;
    }

    public Double getPeriodStartValue() {
        return periodStartValue;
    }

    public PointValueTime getLatestValue() {
        return latestValue;
    }

    public double getComparisonRoCPerMs() {
        return comparisonRoCPerMs;
    }

    public double getResetRoCPerMs() {
        return resetRoCPerMs;
    }

    public long getRocDurationMs() {
        return rocDurationMs;
    }

    public boolean isRocBreachActive() {
        return rocBreachActive;
    }

    public long getRocBreachActiveTime() {
        return rocBreachActiveTime;
    }

    public long getRocBreachInactiveTime() {
        return rocBreachInactiveTime;
    }

    public double getLatestRoc() {
        return latestRoc;
    }

    @Override
    public void initializeState() {
        long time = timer.currentTimeMillis();
        rt = Common.runtimeManager.getDataPoint(vo.getDataPoint().getId());

        //Initialize our period
        //periodStartTime = time; //Use if we don't want to calculate while window is filling
        periodStartTime = computePeriodStart(time); //Use if we want to calculate while the window is filling up

        if(vo.getCalculationMode() == CalculationMode.AVERAGE) {
            comparisonRoCPerMs = vo.getRateOfChangeThreshold() / Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), 1);
            rocDurationMs = Common.getMillis(vo.getRateOfChangePeriodType(), vo.getRateOfChangePeriods());
            if(vo.isUseResetThreshold())
                resetRoCPerMs = vo.getResetThreshold() / Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), 1);

            //Go back duration + averaging period loop over all values to get state
            periodStartTime = time - (getDurationMS() + rocDurationMs);
            currentValueFunction = (l) -> { return getValueAtOrBefore(l);};
            List<PointValueTime> history = rt.getPointValues(periodStartTime);
            if(history.size() > 0) {
                //Swap in simulation timer
                SimulationTimer simTimer = new SimulationTimer();
                simTimer.setStartTime(periodStartTime);
                timer = simTimer;

                for(PointValueTime past : history) {
                    simTimer.fastForwardTo(past.getTime());
                    pointUpdated(past);
                }
                simTimer.fastForwardTo(time);
                //Reset
                timer = Common.timer;
                currentValueFunction = (l) -> {return rt.getPointValue();};

                //Reset our timeout task if necessary
                if(isJobScheduled()) {
                    rescheduleJob();
                }

                List<RocTimeoutTask> reschedule = new ArrayList<>(rocTimeoutTasks);
                rocTimeoutTasks.clear();
                for(RocTimeoutTask rocTimeoutTask : reschedule) {
                    rocTimeoutTask.cancel();
                    RocTimeoutTask task = new RocTimeoutTask(rocTimeoutTask.date);
                    rocTimeoutTasks.add(task);
                    task.schedule();
                }
            }
            currentValueFunction = (l) -> {return rt.getPointValue();};
        }else {
            comparisonRoCPerMs = vo.getRateOfChangeThreshold() / Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), 1);
            if(vo.isUseResetThreshold())
                resetRoCPerMs = vo.getResetThreshold() / Common.getMillis(vo.getRateOfChangeThresholdPeriodType(), 1);

            //Determine our start state if we are using a duration
            long duration = getDurationMS();
            if(duration > 0) {
                long historyStartTime = time - duration;
                List<PointValueTime> history = rt.getPointValues(historyStartTime);
                //Swap in simulation timer
                SimulationTimer simTimer = new SimulationTimer();
                simTimer.setStartTime(historyStartTime);
                timer = simTimer;

                for(PointValueTime past : history) {
                    simTimer.fastForwardTo(past.getTime());
                    pointUpdated(past);
                }
                simTimer.fastForwardTo(time);

                //Reset timer
                timer = Common.timer;

                //Reset our timeout task if necessary
                if(isJobScheduled()) {
                    rescheduleJob();
                }
            }else {
                //Simple initialize of our value, to await our next value
                latestValue = rt.getPointValue();
            }
        }
    }


    @Override
    protected String getThreadNameImpl() {
        return "RateOfChangeDetector " + vo.getXid();
    }

    @Override
    protected TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        TranslatableMessage comparison = vo.getComparisonDescription();
        TranslatableMessage durationDesc = vo.getDurationDescription();
        TranslatableMessage rateOfChangeDurationDesc = vo.getRateOfChangeDurationDescription();

        if (vo.getCalculationMode() == CalculationMode.INSTANTANEOUS) {
            if(durationDesc == null)
                return new TranslatableMessage("event.detector.rocInstantaneous", name, comparison);
            else
                return new TranslatableMessage("event.detector.rocInstantaneousDuration", name, comparison, durationDesc);
        }else {
            if(durationDesc == null)
                return new TranslatableMessage("event.detector.rocAverage", name, comparison, rateOfChangeDurationDesc);
            else
                return new TranslatableMessage("event.detector.rocAverageDuration", name, comparison, rateOfChangeDurationDesc, durationDesc);
        }

    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    private void changeRocBreachActive() {
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
        long now = timer.currentTimeMillis();
        if(vo.getCalculationMode() == CalculationMode.INSTANTANEOUS) {
            if(latestValue == null) {
                //First value ever
                periodStartValue = newValue.getDoubleValue();
                periodStartTime = newValue.getTime();
            }else {
                //Slide values
                periodStartValue = latestValue.getDoubleValue();
                periodStartTime = latestValue.getTime();
            }
            latestValue = newValue;
            rocChanged(now);
        }else {
            latestValue = newValue;
            rocCheckTimeout(now, newValue);
            //Schedule timeout task in case we don't get any more updates
            scheduleRocTimeoutTask(now);
        }
    }

    private synchronized void rocChanged(long now) {
        //Don't compute if there is not a period start value
        if(periodStartValue == null)
            return;

        if(latestValue != null) {
            double currentRoc = firstLastRocAlgorithm();
            this.latestRoc = currentRoc;
            checkState(currentRoc, now, latestValue.getTime());
        }else {
            //Assume 0 RoC
            this.latestRoc = 0.0d;
            checkState(0.0d, now, now);
        }
    }

    private synchronized void checkState(double currentRoc, long fireTime, long latestValueTime) {

        if(vo.isUseAbsoluteValue()) {
            currentRoc = Math.abs(currentRoc);
            this.latestRoc = currentRoc;
        }

        if(vo.getComparisonMode() == ComparisonMode.LESS_THAN_OR_EQUALS){
            if (currentRoc <= comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeRocBreachActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    if ((rocBreachActive)&&(currentRoc >= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }else{
                    //Not using reset
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }
            }
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN){
            if (currentRoc > comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeRocBreachActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    //Turn off alarm if we are active and below the Reset value
                    if ((rocBreachActive) &&(currentRoc <= resetRoCPerMs)){
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }else{
                    //Turn off alarm if we are active
                    if (rocBreachActive){
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }
            }
        }else if(vo.getComparisonMode() == ComparisonMode.GREATER_THAN_OR_EQUALS){
            //Not Lower than
            if (currentRoc >= comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeRocBreachActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    if ((rocBreachActive) && (currentRoc <= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }else{
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }
            }
        }else{ //mode must be LESS_THAN
            //is lower than
            if (currentRoc < comparisonRoCPerMs) {
                if (!rocBreachActive) {
                    rocBreachActiveTime = latestValueTime;
                    changeRocBreachActive();
                }
            }
            else {
                //Are we using a reset value
                if(vo.isUseResetThreshold()){
                    if ((rocBreachActive) && (currentRoc >= resetRoCPerMs)) {
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }else{
                    if (rocBreachActive) {
                        rocBreachInactiveTime = latestValueTime;
                        changeRocBreachActive();
                    }
                }
            }
        }
    }

    synchronized private void rocCheckTimeout(long fireTime, PointValueTime currentValue) {
        long latestTime = fireTime;
        //Slide the window
        periodStartTime = computePeriodStart(fireTime);
        PointValueTime start = getValueAtOrBefore(periodStartTime);
        if(start != null) {
            periodStartValue = start.getDoubleValue();
        }else {
            periodStartValue = null;
        }

        if(currentValue != null && currentValue.getTime() >= periodStartTime) {
            if(latestValue != null && latestValue.getTime() < currentValue.getTime()) {
                latestTime = currentValue.getTime();
            }
            latestValue = currentValue;
        }else {
            latestValue = null;
        }

        // Don't compute if there is not a period start value
        if (periodStartValue == null)
            return;

        if (latestValue != null) {
            double currentRoc = firstLastRocAlgorithm();
            this.latestRoc = currentRoc;
            checkState(currentRoc, fireTime, latestTime);
        } else {
            // Assume 0 RoC
            this.latestRoc = 0.0d;
            checkState(0.0d, fireTime, latestTime);
        }
    }

    private PointValueTime getValueAtOrBefore(long time) {
        PointValueTime start = rt.getPointValueAt(time);
        if(start == null)
            start = rt.getPointValueBefore(time);
        return start;
    }

    synchronized private void cancelRocTimeoutTasks() {
        for(RocTimeoutTask rocTimeoutTask : rocTimeoutTasks) {
            rocTimeoutTask.cancel();
        }
    }

    synchronized private void removeRocTimeoutTask(RocTimeoutTask task) {
        rocTimeoutTasks.remove(task);
    }

    synchronized private void scheduleRocTimeoutTask(long now) {
        RocTimeoutTask task = new RocTimeoutTask(new Date(now + this.rocDurationMs));
        rocTimeoutTasks.add(task);
        task.schedule();
    }

    @Override
    public long getConditionActiveTime() {
        return rocBreachActiveTime;
    }

    @Override
    protected void setEventInactive(long timestamp) {
        this.eventActive = false;
        returnToNormal(rocBreachInactiveTime);
    }

    @Override
    protected synchronized void setEventActive(long timestamp) {
        this.eventActive = rocBreachActive;
        // Just for the fun of it, make sure that there is a breach
        if (rocBreachActive)
            raiseEvent(rocBreachActiveTime + getDurationMS(), createEventContext());
        else {
            // Perhaps the job wasn't successfully unscheduled. Write a log entry and ignore.
            log.warn("Call to set event active when roc detector is not active. Ignoring.");
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
            return (latestValue.getDoubleValue() - periodStartValue)/rocDurationMs;
        else {
            long duration = latestValue.getTime() - periodStartTime;
            if(duration > 0)
                return (latestValue.getDoubleValue() - periodStartValue)/duration;
            else
                return 0; //TODO what do we do here?
        }
    }

    @Override
    public void terminate() {
        cancelRocTimeoutTasks();
        super.terminate();
    }

    /**
     * Schedule a timeout to check the RoC when no point updates are received
     *  then cleanup by removing our reference
     * @author Terry Packer
     *
     */
    class RocTimeoutTask extends TimerTask {

        Date date;

        public RocTimeoutTask(Date date) {
            super(new OneTimeTrigger(date), "ROCD" + vo.getXid(), null, -1);
            this.date = date;
        }

        @Override
        public void run(long runtime) {
            rocCheckTimeout(runtime, currentValueFunction.apply(runtime));
            removeRocTimeoutTask(this);
        }

        public void schedule() {
            timer.schedule(this);
        }

    }
}
