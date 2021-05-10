/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt.event.type;

import java.util.Map;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Placeholder for events types that were module defined and the module was deleted
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

    public String getMissingTypeName() {
        return missingTypeName;
    }

    public void setMissingTypeName(String missingTypeName) {
        this.missingTypeName = missingTypeName;
    }

    public String getMissingSubTypeName() {
        return missingSubTypeName;
    }

    public void setMissingSubTypeName(String missingSubTypeName) {
        this.missingSubTypeName = missingSubTypeName;
    }

    @Override
    public String getEventType() {
        return EventType.EventTypeNames.MISSING;
    }

    @Override
    public String getEventSubtype() {
        return null;
    }

    @Override
    public DuplicateHandling getDuplicateHandling() {
        return DuplicateHandling.IGNORE;
    }

    @Override
    public int getReferenceId1() {
        return ref1;
    }

    @Override
    public int getReferenceId2() {
        return ref2;
    }

    @Override
    public boolean hasPermission(PermissionHolder user, PermissionService service) {
        return service.hasAdminRole(user) || service.hasEventsSuperadminViewPermission(user);
    }

    @Override
    public MangoPermission getEventPermission(Map<String, Object> context, PermissionService service) {
        return MangoPermission.superadminOnly();
    }
}
