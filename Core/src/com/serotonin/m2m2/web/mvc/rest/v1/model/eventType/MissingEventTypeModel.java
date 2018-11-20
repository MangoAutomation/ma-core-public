/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.MissingEventType;
import com.serotonin.m2m2.rt.event.type.EventType.DuplicateHandling;

/**
 *
 * @author Terry Packer
 */
public class MissingEventTypeModel extends EventTypeModel{
    
    private String missingTypeName;
    private String missingSubTypeName;
    private int ref1;
    private int ref2;
    
    public MissingEventTypeModel() { }
    
    public MissingEventTypeModel(String missingTypeName, String missingSubTypeName, int ref1, int ref2) {
        this.missingTypeName = missingTypeName;
        this.missingSubTypeName = missingSubTypeName;
        this.ref1 = ref1;
        this.ref2 = ref2;
    }
    
    /**
     * @return the missingTypeName
     */
    public String getMissingTypeName() {
        return missingTypeName;
    }

    /**
     * @param missingTypeName the missingTypeName to set
     */
    public void setMissingTypeName(String missingTypeName) {
        this.missingTypeName = missingTypeName;
    }

    /**
     * @return the missingSubTypeName
     */
    public String getMissingSubTypeName() {
        return missingSubTypeName;
    }

    /**
     * @param missingSubTypeName the missingSubTypeName to set
     */
    public void setMissingSubTypeName(String missingSubTypeName) {
        this.missingSubTypeName = missingSubTypeName;
    }

    public int getReferenceId1() {
        return ref1;
    }
    
    public void setReferenceId1(int ref1) {
        this.ref1 = ref1;
    }
    
    public int getReferenceId2() {
        return ref2;
    }
    
    public void setReferenceId2(int ref2) {
        this.ref2 = ref2;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getTypeName()
     */
    @Override
    public String getTypeName() {
        return EventType.EventTypeNames.MISSING;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#isRateLimited()
     */
    @Override
    public boolean isRateLimited() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getDuplicateHandling()
     */
    @Override
    public String getDuplicateHandling() {
        return EventType.DUPLICATE_HANDLING_CODES.getCode(DuplicateHandling.DO_NOT_ALLOW);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getEventTypeInstance()
     */
    @Override
    public EventType toEventType() {
        return new MissingEventType(missingTypeName, missingSubTypeName, ref1, ref2);
    }

}
