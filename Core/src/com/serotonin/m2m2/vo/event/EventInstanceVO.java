/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.LazyField;
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
public class EventInstanceVO extends AbstractVO implements EventInstanceI {

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
    private List<UserCommentVO> eventComments = Collections.emptyList();

    private Long acknowledgedTimestamp;
    private Integer acknowledgedByUserId;
    private String acknowledgedByUsername;
    private TranslatableMessage alternateAckSource;
    private boolean hasComments;
    private LazyField<MangoPermission> readPermission = new LazyField<>(new MangoPermission());

    @Override
    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public long getActiveTimestamp() {
        return activeTimestamp;
    }

    public void setActiveTimestamp(long activeTimestamp) {
        this.activeTimestamp = activeTimestamp;
    }

    @Override
    public boolean isRtnApplicable() {
        return rtnApplicable;
    }

    public void setRtnApplicable(boolean rtnApplicable) {
        this.rtnApplicable = rtnApplicable;
    }

    @Override
    public Long getRtnTimestamp() {
        return rtnTimestamp;
    }

    public void setRtnTimestamp(Long rtnTimestamp) {
        this.rtnTimestamp = rtnTimestamp;
    }

    @Override
    public ReturnCause getRtnCause() {
        return rtnCause;
    }


    public void setRtnCause(ReturnCause rtnCause) {
        this.rtnCause = rtnCause;
    }

    @Override
    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(AlarmLevels alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    @Override
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

    @Override
    public List<UserCommentVO> getEventComments() {
        return eventComments;
    }

    public void setEventComments(List<UserCommentVO> eventComments) {
        this.eventComments = Objects.requireNonNull(eventComments);
    }

    @Override
    public Long getAcknowledgedTimestamp() {
        return acknowledgedTimestamp;
    }

    public void setAcknowledgedTimestamp(Long acknowledgedTimestamp) {
        this.acknowledgedTimestamp = acknowledgedTimestamp;
    }

    @Override
    public Integer getAcknowledgedByUserId() {
        return acknowledgedByUserId;
    }

    public void setAcknowledgedByUserId(Integer acknowledgedByUserId) {
        this.acknowledgedByUserId = acknowledgedByUserId;
    }

    @Override
    public String getAcknowledgedByUsername() {
        return acknowledgedByUsername;
    }

    public void setAcknowledgedByUsername(String acknowledgedByUsername) {
        this.acknowledgedByUsername = acknowledgedByUsername;
    }

    @Override
    public TranslatableMessage getAlternateAckSource() {
        return alternateAckSource;
    }

    public void setAlternateAckSource(TranslatableMessage alternateAckSource) {
        this.alternateAckSource = alternateAckSource;
    }

    @Override
    public boolean isHasComments() {
        return hasComments;
    }

    public void setHasComments(boolean hasComments) {
        this.hasComments = hasComments;
    }

    public MangoPermission getReadPermission() {
        return readPermission.get();
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission.set(readPermission);
    }

    public void supplyReadPermission(Supplier<MangoPermission> readPermission) {
        this.readPermission = new LazyField<MangoPermission>(readPermission);
    }

    public boolean isActive() {
        return rtnApplicable && rtnTimestamp == null;
    }

    @Override
    public String getTypeKey() {
        return "common.default"; //TODO Currently No Audit Events for this
    }

}
