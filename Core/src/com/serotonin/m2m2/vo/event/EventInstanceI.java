/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;


/**
 * @author Jared Wiltshire
 */
public interface EventInstanceI {

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

}
