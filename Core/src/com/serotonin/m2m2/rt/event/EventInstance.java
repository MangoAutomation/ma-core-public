/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.Functions;
import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.rt.event.type.MissingEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.event.EventInstanceI;

public class EventInstance implements EventInstanceI {
    /**
     * Model key for event instance objects in scripts
     */
    public static final String CONTEXT_KEY = "evt";

    /**
     * Configuration field. Assigned by the database.
     */
    private int id = Common.NEW_ID;

    /**
     * Configuration field. Provided by the event producer. Identifies where the event came from and what it means.
     */
    private final EventType eventType;

    /**
     * State field. The time that the event became active (i.e. was raised).
     */
    private final long activeTimestamp;

    /**
     * Configuration field. Is this type of event capable of returning to normal (true), or is it stateless (false).
     */
    private final boolean rtnApplicable;

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
    private final AlarmLevels alarmLevel;

    /**
     * Configuration field. The message associated with the event.
     */
    private final TranslatableMessage message;

    /**
     * User comments on the event. Added in the events interface after the event has been raised.
     */
    private List<UserCommentVO> eventComments = Collections.emptyList();

    private List<EventHandlerRT<?>> handlers = Collections.emptyList();

    private Long acknowledgedTimestamp;
    private Integer acknowledgedByUserId;
    private String acknowledgedByUsername;
    private TranslatableMessage alternateAckSource;
    private boolean hasComments;

    private LazyField<MangoPermission> readPermission = new LazyField<>(new MangoPermission());

    //Used so that the multicaster knows what to ignore
    private List<Integer> idsToNotify;

    //
    // Contextual data from the source that raised the event.
    private final Map<String, Object> context;

    public EventInstance(EventType eventType, long activeTimestamp, boolean rtnApplicable, AlarmLevels alarmLevel,
            TranslatableMessage message, Map<String, Object> context) {
        this.eventType = eventType;
        this.activeTimestamp = activeTimestamp;
        this.rtnApplicable = rtnApplicable;
        this.alarmLevel = alarmLevel;
        if (message == null)
            this.message = new TranslatableMessage("common.noMessage");
        else
            this.message = message;
        this.context = context == null ? Collections.emptyMap() : context;
    }

    public TranslatableMessage getAckMessage() {
        if (isAcknowledged()) {
            if (acknowledgedByUserId != null)
                return new TranslatableMessage("events.ackedByUser", acknowledgedByUsername);
            if (alternateAckSource != null)
                return alternateAckSource;
        }

        return null;
    }

    public TranslatableMessage getExportAckMessage() {
        if (isAcknowledged()) {
            if (acknowledgedByUserId != null)
                return new TranslatableMessage("events.export.ackedByUser", acknowledgedByUsername);
            if (alternateAckSource != null)
                return alternateAckSource;
        }

        return null;
    }

    public String getPrettyActiveTimestamp() {
        return Functions.getTime(activeTimestamp);
    }

    public String getFullPrettyActiveTimestamp() {
        return Functions.getFullSecondTime(activeTimestamp);
    }

    public String getPrettyRtnTimestamp() {
        return Functions.getTime(rtnTimestamp);
    }

    public String getFullPrettyRtnTimestamp() {
        return Functions.getFullSecondTime(rtnTimestamp);
    }

    public String getFullPrettyAcknowledgedTimestamp() {
        return Functions.getFullSecondTime(acknowledgedTimestamp);
    }

    /**
     * This method should only be used by the EventDao for creating and updating.
     *
     */
    public void setId(int id) {
        this.id = id;
    }

    public boolean isActive() {
        return rtnApplicable && rtnTimestamp == null;
    }

    public void returnToNormal(long time, ReturnCause rtnCause) {
        if (isActive()) {
            rtnTimestamp = time;
            this.rtnCause = rtnCause;
        }
    }

    public boolean isAcknowledged() {
        return acknowledgedTimestamp != null;
    }

    @Override
    public long getActiveTimestamp() {
        return activeTimestamp;
    }

    @Override
    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Long getRtnTimestamp() {
        return rtnTimestamp;
    }

    @Override
    public TranslatableMessage getMessage() {
        if(eventType.getEventType().equals(EventTypeNames.MISSING)) {
            MissingEventType type = (MissingEventType)eventType;
            return new TranslatableMessage("event.missing", type.getMissingTypeName());
        }else
            return message;
    }

    public String getMessageString(){
        if(eventType.getEventType().equals(EventTypeNames.MISSING)) {
            MissingEventType type = (MissingEventType)eventType;
            return new TranslatableMessage("event.missing", type.getMissingTypeName()).translate(Common.getTranslations());
        }else
            return message.translate(Common.getTranslations());
    }

    @Override
    public boolean isRtnApplicable() {
        return rtnApplicable;
    }

    public void setEventComments(List<UserCommentVO> eventComments) {
        this.eventComments = Objects.requireNonNull(eventComments);
    }

    @Override
    public List<UserCommentVO> getEventComments() {
        return eventComments;
    }

    @Override
    public ReturnCause getRtnCause() {
        return rtnCause;
    }

    @NonNull
    public List<EventHandlerRT<?>> getHandlers() {
        return handlers;
    }

    public void setHandlers(@NonNull List<EventHandlerRT<?>> handlers) {
        this.handlers = Objects.requireNonNull(handlers);
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

    public Map<String, Object> getContext() {
        return context;
    }

    public Object context(String key) {
        return context.get(key);
    }

    public List<Integer> getIdsToNotify() {
        return idsToNotify;
    }

    public void setIdsToNotify(List<Integer> idsToNotify) {
        this.idsToNotify = idsToNotify;
    }

    @Override
    public String toString() {
        return "EventInstance [id=" + id + ", eventType=" + eventType + ", activeTimestamp="
                + activeTimestamp + ", rtnApplicable=" + rtnApplicable + ", rtnTimestamp="
                + rtnTimestamp + ", rtnCause=" + rtnCause + ", alarmLevel=" + alarmLevel
                + ", message=" + message + ", eventComments=" + eventComments + ", handlers="
                + handlers + ", acknowledgedTimestamp=" + acknowledgedTimestamp
                + ", acknowledgedByUserId=" + acknowledgedByUserId + ", acknowledgedByUsername="
                + acknowledgedByUsername + ", alternateAckSource=" + alternateAckSource
                + ", hasComments=" + hasComments + ", idsToNotify=" + idsToNotify + ", context=" + context + "]";
    }

}
