/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.event;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.EventType;

public class EventTypeVO {
    
    private final EventType eventType;
    
    private TranslatableMessage description;
    private int alarmLevel;

//    public EventTypeVO() {
//        
//    }
    
    public EventTypeVO(EventType eventType, TranslatableMessage description,
            int alarmLevel) {
        this.eventType = eventType;
        this.description = description;
        this.alarmLevel = alarmLevel;
    }

    public EventType getEventType() {
        return eventType;
    }

    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public String getType() {
        return eventType.getEventType();
    }

//    public void setType(String type) {
//        this.type = type;
//    }
    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public String getSubtype() {
        return eventType.getEventSubtype();
    }

//    public void setSubtype(String subtype) {
//        this.subtype = subtype;
//    }
    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public int getTypeRef1() {
        return eventType.getReferenceId1();
    }

//    public void setTypeRef1(int typeRef1) {
//        this.typeRef1 = typeRef1;
//    }
    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public int getTypeRef2() {
        return eventType.getReferenceId2();
    }

//    public void setTypeRef2(int typeRef2) {
//        this.typeRef2 = typeRef2;
//    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public void setDescription(TranslatableMessage description) {
        this.description = description;
    }

    public int getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(int alarmLevel) {
        this.alarmLevel = alarmLevel;
    }
}
