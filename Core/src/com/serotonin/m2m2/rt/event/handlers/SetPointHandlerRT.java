/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.SetPointWorkItem;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

public class SetPointHandlerRT extends EventHandlerRT<SetPointEventHandlerVO> implements SetPointSource {
    private static final Log LOG = LogFactory.getLog(SetPointHandlerRT.class);

    public SetPointHandlerRT(SetPointEventHandlerVO vo) {
        super(vo);
    }

    @Override
    public void eventRaised(EventInstance evt) {
        if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE)
            return;

        // Validate that the target point is available.
        DataPointRT targetPoint = Common.runtimeManager.getDataPoint(vo.getTargetPointId());
        if (targetPoint == null) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetPointMissing"), evt.getEventType());
            return;
        }

        if (!targetPoint.getPointLocator().isSettable()) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetNotSettable"), evt.getEventType());
            return;
        }

        int targetDataType = targetPoint.getVO().getPointLocator().getDataTypeId();

        DataValue value;
        if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            // Get the source data point.
            DataPointRT sourcePoint = Common.runtimeManager.getDataPoint(vo.getActivePointId());
            if (sourcePoint == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.activePointMissing"), evt.getEventType());
                return;
            }

            PointValueTime valueTime = sourcePoint.getPointValue();
            if (valueTime == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.activePointValue"), evt.getEventType());
                return;
            }

            if (DataTypes.getDataType(valueTime.getValue()) != targetDataType) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.activePointDataType"), evt.getEventType());
                return;
            }

            value = valueTime.getValue();
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE) {
            value = DataValue.stringToValue(vo.getActiveValueToSet(), targetDataType);
        }
        else
            throw new ShouldNeverHappenException("Unknown active action: " + vo.getActiveAction());

        // Queue a work item to perform the set point.
        Common.backgroundProcessing.addWorkItem(new SetPointWorkItem(vo.getTargetPointId(), new PointValueTime(value,
                evt.getActiveTimestamp()), this));
    }

    @Override
    public void eventInactive(EventInstance evt) {
        if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE)
            return;

        // Validate that the target point is available.
        DataPointRT targetPoint = Common.runtimeManager.getDataPoint(vo.getTargetPointId());
        if (targetPoint == null) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetPointMissing"), evt.getEventType());
            return;
        }

        if (!targetPoint.getPointLocator().isSettable()) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetNotSettable"), evt.getEventType());
            return;
        }

        int targetDataType = targetPoint.getVO().getPointLocator().getDataTypeId();

        DataValue value;
        if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            // Get the source data point.
            DataPointRT sourcePoint = Common.runtimeManager.getDataPoint(vo.getInactivePointId());
            if (sourcePoint == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.inactivePointMissing"), evt.getEventType());
                return;
            }

            PointValueTime valueTime = sourcePoint.getPointValue();
            if (valueTime == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.inactivePointValue"), evt.getEventType());
                return;
            }

            if (DataTypes.getDataType(valueTime.getValue()) != targetDataType) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.inactivePointDataType"), evt.getEventType());
                return;
            }

            value = valueTime.getValue();
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE)
            value = DataValue.stringToValue(vo.getInactiveValueToSet(), targetDataType);
        else
            throw new ShouldNeverHappenException("Unknown active action: " + vo.getInactiveAction());

        Common.backgroundProcessing.addWorkItem(new SetPointWorkItem(vo.getTargetPointId(), new PointValueTime(value,
                evt.getRtnTimestamp()), this));
    }

    private void raiseFailureEvent(TranslatableMessage message, EventType et) {
        if (et != null && et.isSystemMessage()) {
            if (((SystemEventType) et).getSystemEventType().equals(SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE)) {
                // The set point attempt failed for an event that is a set point handler failure in the first place.
                // Do not propagate the event, but rather just write a log message.
                LOG.warn("A set point event due to a set point handler failure itself failed. The failure event "
                        + "has been discarded: " + message.translate(Common.getTranslations()));
                return;
            }
        }

        SystemEventType eventType = new SystemEventType(SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE, vo.getId());
        if (StringUtils.isBlank(vo.getAlias()))
            message = new TranslatableMessage("event.setPointFailed", message);
        else
            message = new TranslatableMessage("event.setPointFailed.alias", vo.getAlias(), message);
        SystemEventType.raiseEvent(eventType, System.currentTimeMillis(), false, message);
    }

    //
    // SetPointSource implementation
    //
    @Override
    public String getSetPointSourceType() {
        return "SET_POINT_EVENT_HANDLER";
    }

    public int getSetPointSourceId() {
        return vo.getId();
    }

    @Override
    public TranslatableMessage getSetPointSourceMessage() {
        return new TranslatableMessage("annotation.eventHandler");
    }

    public void raiseRecursionFailureEvent() {
        raiseFailureEvent(new TranslatableMessage("event.setPoint.recursionFailure"), null);
    }
}
