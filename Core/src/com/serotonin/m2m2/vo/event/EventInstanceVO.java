/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.util.Functions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
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
    private long rtnTimestamp;

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

    private List<EventHandlerRT<?>> handlers;

    private long acknowledgedTimestamp;
    private int acknowledgedByUserId;
    private String acknowledgedByUsername;
    private TranslatableMessage alternateAckSource;
    private boolean hasComments;

    //
    // Contextual data from the source that raised the event.
    private Map<String, Object> context;

    /**
     * Total Time for alarm since conception to rtn or now
     * if not complete
     */
    private Long totalTime;

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


    public long getRtnTimestamp() {
        return rtnTimestamp;
    }


    public void setRtnTimestamp(long rtnTimestamp) {
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


    public String getMessageString(){
        if(eventType.getEventType() == EventType.EventTypeNames.MISSING) {
            MissingEventType type = (MissingEventType)eventType;
            return new TranslatableMessage("event.missing", type.getMissingTypeName()).translate(Common.getTranslations());
        }else
            return message.translate(Common.getTranslations());
    }

    public void setMessageString(String msg){
        //NoOp
    }

    public List<UserCommentVO> getEventComments() {
        return eventComments;
    }


    public void setEventComments(List<UserCommentVO> eventComments) {
        this.eventComments = eventComments;
    }


    public List<EventHandlerRT<?>> getHandlers() {
        return handlers;
    }


    public void setHandlers(List<EventHandlerRT<?>> handlers) {
        this.handlers = handlers;
    }


    public long getAcknowledgedTimestamp() {
        return acknowledgedTimestamp;
    }


    public void setAcknowledgedTimestamp(long acknowledgedTimestamp) {
        this.acknowledgedTimestamp = acknowledgedTimestamp;
    }

    public String getAcknowledgedTimestampString(){
        return Functions.getFullSecondTime(this.acknowledgedTimestamp);
    }

    public void setAcknowledgedTimestamp(String s){
        //NoOp
    }

    public int getAcknowledgedByUserId() {
        return acknowledgedByUserId;
    }

    public void setAcknowledgedByUserId(int acknowledgedByUserId) {
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

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public void setActive(boolean active){
        //NoOp
    }
    public boolean isActive() {
        return rtnApplicable && rtnTimestamp == 0;
    }

    public void setRtnMessageString(String msg){
        //NoOp
    }

    public String getRtnMessageString() {
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
        if(rtnKey != null)
            return rtnKey.translate(Common.getTranslations());
        else
            return "";
    }

    public void setAcknowledged(boolean ack){
        //NoOp
    }
    public boolean isAcknowledged() {
        return acknowledgedTimestamp > 0;
    }

    public void setTotalTime(Long totalTime){
        this.totalTime = totalTime;
    }
    public Long getTotalTime(){
        return totalTime;
    }

    @Override
    public String getTypeKey() {
        return null; //TODO Currently No Audit Events for this
    }

}
