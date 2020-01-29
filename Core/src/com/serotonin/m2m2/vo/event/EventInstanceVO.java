/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.MissingEventType;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.comment.UserCommentVO;

/**
 * @author Terry Packer
 *
 */
public class EventInstanceVO extends AbstractVO {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Configuration field. Provided by the event producer. Identifies where the event came from and what it means.
     */
    private EventType eventType;

    /**
     * State field. The time that the event became active (i.e. was raised).
     */
    private long activeTimestamp;

    /**
     * Configuration field. Is this type of event capable of returning to normal (true), or is it stateless (false).
     */
    private boolean rtnApplicable;

    /**
     * State field. The time that the event returned to normal.
     */
    private Long rtnTimestamp;

    /**
     * State field. The action that caused the event to RTN. One of {@link ReturnCause}
     */
    private ReturnCause rtnCause;

    /**
     * Configuration field. The alarm level assigned to the event.
     *
     * @see AlarmLevels
     */
    private AlarmLevels alarmLevel;

    /**
     * Configuration field. The message associated with the event.
     */
    private TranslatableMessage message;

    /**
     * User comments on the event. Added in the events interface after the event has been raised.
     */
    private List<UserCommentVO> eventComments;

    private Long acknowledgedTimestamp;
    private Integer acknowledgedByUserId;
    private String acknowledgedByUsername;
    private TranslatableMessage alternateAckSource;
    private boolean hasComments;

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public long getActiveTimestamp() {
        return activeTimestamp;
    }

    public void setActiveTimestamp(long activeTimestamp) {
        this.activeTimestamp = activeTimestamp;
    }

    public boolean isRtnApplicable() {
        return rtnApplicable;
    }

    public void setRtnApplicable(boolean rtnApplicable) {
        this.rtnApplicable = rtnApplicable;
    }

    public Long getRtnTimestamp() {
        return rtnTimestamp;
    }

    public void setRtnTimestamp(Long rtnTimestamp) {
        this.rtnTimestamp = rtnTimestamp;
    }

    public ReturnCause getRtnCause() {
        return rtnCause;
    }


    public void setRtnCause(ReturnCause rtnCause) {
        this.rtnCause = rtnCause;
    }

    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(AlarmLevels alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public TranslatableMessage getMessage() {
        if(eventType.getEventType() == EventType.EventTypeNames.MISSING) {
            MissingEventType type = (MissingEventType)eventType;
            return new TranslatableMessage("event.missing", type.getMissingTypeName());
        }else
            return message;
    }

    public void setMessage(TranslatableMessage message) {
        this.message = message;
    }

    public List<UserCommentVO> getEventComments() {
        return eventComments;
    }

    public void setEventComments(List<UserCommentVO> eventComments) {
        this.eventComments = eventComments;
    }

    public Long getAcknowledgedTimestamp() {
        return acknowledgedTimestamp;
    }

    public void setAcknowledgedTimestamp(Long acknowledgedTimestamp) {
        this.acknowledgedTimestamp = acknowledgedTimestamp;
    }

    public Integer getAcknowledgedByUserId() {
        return acknowledgedByUserId;
    }

    public void setAcknowledgedByUserId(Integer acknowledgedByUserId) {
        this.acknowledgedByUserId = acknowledgedByUserId;
    }

    public String getAcknowledgedByUsername() {
        return acknowledgedByUsername;
    }

    public void setAcknowledgedByUsername(String acknowledgedByUsername) {
        this.acknowledgedByUsername = acknowledgedByUsername;
    }

    public TranslatableMessage getAlternateAckSource() {
        return alternateAckSource;
    }

    public void setAlternateAckSource(TranslatableMessage alternateAckSource) {
        this.alternateAckSource = alternateAckSource;
    }

    public boolean isHasComments() {
        return hasComments;
    }

    public void setHasComments(boolean hasComments) {
        this.hasComments = hasComments;
    }

    @Override
    public String getTypeKey() {
        return null; //TODO Currently No Audit Events for this
    }

}
