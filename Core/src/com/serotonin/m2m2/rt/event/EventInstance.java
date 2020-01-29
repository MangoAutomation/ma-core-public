/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event;

import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.util.Functions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.MissingEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;

public class EventInstance{
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
    private List<UserCommentVO> eventComments;

    private List<EventHandlerRT<?>> handlers;

    private Long acknowledgedTimestamp;
    private Integer acknowledgedByUserId;
    private String acknowledgedByUsername;
    private TranslatableMessage alternateAckSource;
    private boolean hasComments;

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
        this.context = context;
    }

    public TranslatableMessage getRtnMessage() {
        TranslatableMessage rtnKey = null;

        if (!isActive()) {
            if (rtnCause == ReturnCause.RETURN_TO_NORMAL)
                rtnKey = new TranslatableMessage("event.rtn.rtn");
            else if (rtnCause == ReturnCause.SOURCE_DISABLED) {
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
            }
            else
                rtnKey = new TranslatableMessage("event.rtn.unknown");
        }

        return rtnKey;
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
     * @param id
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

    public long getActiveTimestamp() {
        return activeTimestamp;
    }

    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getId() {
        return id;
    }

    public long getRtnTimestamp() {
        return rtnTimestamp;
    }

    public TranslatableMessage getMessage() {
        if(eventType.getEventType() == EventType.EventTypeNames.MISSING) {
            MissingEventType type = (MissingEventType)eventType;
            return new TranslatableMessage("event.missing", type.getMissingTypeName());
        }else
            return message;
    }

    public String getMessageString(){
        if(eventType.getEventType() == EventType.EventTypeNames.MISSING) {
            MissingEventType type = (MissingEventType)eventType;
            return new TranslatableMessage("event.missing", type.getMissingTypeName()).translate(Common.getTranslations());
        }else
            return message.translate(Common.getTranslations());
    }

    public boolean isRtnApplicable() {
        return rtnApplicable;
    }

    public void addEventComment(UserCommentVO comment) {
        eventComments.add(comment);
    }

    public void setEventComments(List<UserCommentVO> eventComments) {
        this.eventComments = eventComments;
    }

    public List<UserCommentVO> getEventComments() {
        return eventComments;
    }

    public ReturnCause getRtnCause() {
        return rtnCause;
    }

    public List<EventHandlerRT<?>> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<EventHandlerRT<?>> handlers) {
        this.handlers = handlers;
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

    public Map<String, Object> getContext() {
        return context;
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
