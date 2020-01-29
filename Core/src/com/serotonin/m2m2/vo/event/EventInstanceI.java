/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;


/**
 * @author Jared Wiltshire
 */
public interface EventInstanceI {

    int getId();

    EventType getEventType();

    long getActiveTimestamp();

    boolean isRtnApplicable();

    Long getRtnTimestamp();

    ReturnCause getRtnCause();

    AlarmLevels getAlarmLevel();

    TranslatableMessage getMessage();

    List<UserCommentVO> getEventComments();

    Long getAcknowledgedTimestamp();

    Integer getAcknowledgedByUserId();

    String getAcknowledgedByUsername();

    TranslatableMessage getAlternateAckSource();

    boolean isHasComments();

    public default TranslatableMessage getRtnMessage() {
        TranslatableMessage rtnKey = null;

        EventType eventType = getEventType();
        ReturnCause rtnCause = getRtnCause();

        if (rtnCause != null) {
            switch(rtnCause) {
                case RETURN_TO_NORMAL:
                    rtnKey = new TranslatableMessage("event.rtn.rtn");
                    break;
                case SOURCE_DISABLED:
                    if (eventType.getEventType().equals(EventType.EventTypeNames.DATA_POINT))
                        rtnKey = new TranslatableMessage("event.rtn.pointDisabled");
                    else if (eventType.getEventType().equals(EventType.EventTypeNames.DATA_SOURCE))
                        rtnKey = new TranslatableMessage("event.rtn.dsDisabled");
                    else if (eventType.getEventType().equals(EventType.EventTypeNames.PUBLISHER))
                        rtnKey = new TranslatableMessage("event.rtn.pubDisabled");
                    else {
                        EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(eventType.getEventType());
                        if (def != null)
                            rtnKey = def.getSourceDisabledMessage();
                        if (rtnKey == null)
                            rtnKey = new TranslatableMessage("event.rtn.shutdown");
                    }
                    break;
                default:
                    rtnKey = new TranslatableMessage("event.rtn.unknown");
                    break;
            }
        }

        return rtnKey;
    }

}
