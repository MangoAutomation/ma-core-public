/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt.event.type;

import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.MissingEventTypeModel;

/**
 *
 * @author Terry Packer
 */
public class MissingEventType extends EventType{

    private String missingTypeName;
    private String missingSubTypeName;
    private int ref1;
    private int ref2;

    public MissingEventType() { }

    public MissingEventType(String missingTypeName, String missingSubTypeName, int ref1, int ref2) {
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getEventType()
     */
    @Override
    public String getEventType() {
        return EventType.EventTypeNames.MISSING;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getEventSubtype()
     */
    @Override
    public String getEventSubtype() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getDuplicateHandling()
     */
    @Override
    public int getDuplicateHandling() {
        return EventType.DuplicateHandling.IGNORE;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getReferenceId1()
     */
    @Override
    public int getReferenceId1() {
        return ref1;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getReferenceId2()
     */
    @Override
    public int getReferenceId2() {
        return ref2;
    }

    @Override
    public boolean hasPermission(PermissionHolder user) {
        return Permissions.hasAdminPermission(user);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#asModel()
     */
    @Override
    public EventTypeModel asModel() {
        return new MissingEventTypeModel(missingTypeName, missingSubTypeName, ref1, ref2);
    }

}
