/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

abstract public class PointEventDetectorRT<T extends AbstractPointEventDetectorVO> extends AbstractEventDetectorRT<T> implements DataPointListener {

    public static final String EVENT_DETECTOR_CONTEXT_KEY = "pointEventDetector";
    public static final String DATA_POINT_CONTEXT_KEY = "point";

    public PointEventDetectorRT(T vo) {
        super(vo);
    }

    protected EventType getEventType() {
        DataPointEventType et = new DataPointEventType(vo.getDataPoint(), vo);
        if (!vo.isRtnApplicable())
            et.setDuplicateHandling(DuplicateHandling.ALLOW);
        return et;
    }

    protected void raiseEvent(long time, Map<String, Object> context) {
        TranslatableMessage msg;
        if (!StringUtils.isBlank(vo.getName()))
            msg = new TranslatableMessage("common.default", vo.getName());
        else
            msg = getMessage();

        Common.eventManager.raiseEvent(getEventType(), time, vo.isRtnApplicable(), vo.getAlarmLevel(), msg, context);
    }

    protected void returnToNormal(long time) {
        Common.eventManager.returnToNormal(getEventType(), time);
    }

    protected Map<String, Object> createEventContext() {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(EVENT_DETECTOR_CONTEXT_KEY, vo);
        DataPointVO dataPointVo = vo.getDataPoint();
        context.put(DATA_POINT_CONTEXT_KEY, dataPointVo);
        return context;
    }

    /**
     * Get the message of the event
     * @return
     */
    abstract protected TranslatableMessage getMessage();

    /**
     * Is my event currently active?
     * @return
     */
    public abstract boolean isEventActive();

    @Override
    public String getListenerName(){
        return vo.getXid();
    }

    //
    //
    // Lifecycle interface
    //
    public void initialize() {
        // no op
    }

    public void terminate() {
        // no op
    }

    public void joinTermination() {
        // no op
    }

    //
    //
    // Point listener interface
    //
    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        // no op
    }
    @Override
    public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
        // no op
    }
    @Override
    public void pointUpdated(PointValueTime newValue) {
        // no op
    }
    @Override
    public void pointBackdated(PointValueTime value) {
        // no op
    }
    @Override
    public void pointInitialized() {
        // no op
    }
    @Override
    public void pointTerminated(DataPointVO vo) {
        // no op
    }
    @Override
    public void pointLogged(PointValueTime value){
        //no op
    }
}
