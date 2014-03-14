/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.event;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;

public class EventTypeVO {
    /**
     * The type of event. @see EventType.EventTypeNames
     */
    private String type;
    private String subtype;

    /**
     * For data point event, the data point id. For data source event, the data source id. For system event, depends on
     * the subtype.
     */
    private int typeRef1;

    /**
     * For data point event, the point event detector id. For data source event, the data source event type. For system
     * event, undefined.
     */
    private int typeRef2;
    private TranslatableMessage description;
    private List<EventHandlerVO> handlers;
    private int alarmLevel;
    private String eventDetectorKey;
    private int duplicateHandling;

    public EventTypeVO(String type, String subtype, int typeRef1, int typeRef2) {
        this.type = type;
        this.subtype = subtype;
        this.typeRef1 = typeRef1;
        this.typeRef2 = typeRef2;
    }

    public EventTypeVO(String type, String subtype, int typeRef1, int typeRef2, TranslatableMessage description,
            int alarmLevel) {
        this(type, subtype, typeRef1, typeRef2);
        this.description = description;
        this.alarmLevel = alarmLevel;
    }

    public EventTypeVO(String type, String subtype, int typeRef1, int typeRef2, TranslatableMessage description,
            int alarmLevel, int duplicateHandling) {
        this(type, subtype, typeRef1, typeRef2, description, alarmLevel);
        this.duplicateHandling = duplicateHandling;
    }

    public EventType createEventType() {
        if (type.equals(EventType.EventTypeNames.DATA_POINT))
            return new DataPointEventType(typeRef1, typeRef2);
        if (type.equals(EventType.EventTypeNames.DATA_SOURCE))
            return new DataSourceEventType(typeRef1, typeRef2, alarmLevel, duplicateHandling);
        if (type.equals(EventType.EventTypeNames.SYSTEM))
            return new SystemEventType(subtype, typeRef1);
        if (type.equals(EventType.EventTypeNames.PUBLISHER))
            return new PublisherEventType(typeRef1, typeRef2);
        if (type.equals(EventType.EventTypeNames.AUDIT))
            return new AuditEventType(subtype, typeRef1);

        EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(type);
        if (def != null)
            return def.createEventType(subtype, typeRef1, typeRef2);

        return null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public int getTypeRef1() {
        return typeRef1;
    }

    public void setTypeRef1(int typeRef1) {
        this.typeRef1 = typeRef1;
    }

    public int getTypeRef2() {
        return typeRef2;
    }

    public void setTypeRef2(int typeRef2) {
        this.typeRef2 = typeRef2;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public void setDescription(TranslatableMessage description) {
        this.description = description;
    }

    public List<EventHandlerVO> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<EventHandlerVO> handlers) {
        this.handlers = handlers;
    }

    public int getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(int alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public String getEventDetectorKey() {
        return eventDetectorKey;
    }

    public void setEventDetectorKey(String eventDetectorKey) {
        this.eventDetectorKey = eventDetectorKey;
    }
}
