/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;

abstract public class PointEventDetectorRT implements DataPointListener {
    protected PointEventDetectorVO vo;

    protected EventType getEventType() {
        DataPointEventType et = new DataPointEventType(vo.njbGetDataPoint().getId(), vo.getId());
        if (!vo.isRtnApplicable())
            et.setDuplicateHandling(EventType.DuplicateHandling.ALLOW);
        return et;
    }

    protected void raiseEvent(long time, Map<String, Object> context) {
        TranslatableMessage msg;
        if (!StringUtils.isBlank(vo.getAlias()))
            msg = new TranslatableMessage("common.default", vo.getAlias());
        else
            msg = getMessage();

        Common.eventManager.raiseEvent(getEventType(), time, vo.isRtnApplicable(), vo.getAlarmLevel(), msg, context);
    }

    protected void returnToNormal(long time) {
        Common.eventManager.returnToNormal(getEventType(), time);
    }

    protected Map<String, Object> createEventContext() {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("pointEventDetector", vo);
        context.put("point", vo.njbGetDataPoint());
        return context;
    }

    abstract protected TranslatableMessage getMessage();

    abstract protected boolean isEventActive();

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
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        // no op
    }

    public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
        // no op
    }

    public void pointUpdated(PointValueTime newValue) {
        // no op
    }

    public void pointBackdated(PointValueTime value) {
        // no op
    }

    public void pointInitialized() {
        // no op
    }

    public void pointTerminated() {
        // no op
    }
}
