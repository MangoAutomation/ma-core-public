/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type;

import java.util.Map;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class MockEventType extends EventType {

    public static final String TYPE_NAME = "MOCK";
    private DuplicateHandling duplicateHandling;
    private String eventSubtype;
    private int ref1;
    private int ref2;
    private Role required;

    /**
     * Create a mock event type with duplicate handling of ALLOW
     * and event sub type of null with required role
     */
    public MockEventType(Role required) {
        this(DuplicateHandling.ALLOW);
        this.required = required;
    }

    /**
     * default subtype of null and no role
     * @param duplicateHandling
     */
    public MockEventType(DuplicateHandling duplicateHandling) {
        this(duplicateHandling, null);
    }

    /**
     * No role applied (admin only)
     * @param duplicateHandling
     * @param eventSubType
     */
    public MockEventType(DuplicateHandling duplicateHandling, String eventSubType) {
        this(duplicateHandling, eventSubType, -1, -1, null);
    }

    public MockEventType(DuplicateHandling duplicateHandling, String eventSubType, int ref1, int ref2, Role required) {
        this.duplicateHandling = duplicateHandling;
        this.eventSubtype = null;
        this.ref1 = ref1;
        this.ref2 = ref2;
        this.required = required;
    }

    @Override
    public String getEventType() {
        return TYPE_NAME;
    }

    @Override
    public String getEventSubtype() {
        return eventSubtype;
    }

    @Override
    public DuplicateHandling getDuplicateHandling() {
        return duplicateHandling;
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
        return service.hasAdminRole(user) || service.hasPermission(user, MangoPermission.requireAnyRole(this.required));
    }

    @Override
    public MangoPermission getEventPermission(Map<String, Object> context, PermissionService service) {
        if(required != null) {
            return MangoPermission.builder().minterm(required).build();
        }else {
            return MangoPermission.superadminOnly();
        }
    }

    @Override
    public int getDataPointId() {
        return ref2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((duplicateHandling == null) ? 0 : duplicateHandling.hashCode());
        result = prime * result + ((eventSubtype == null) ? 0 : eventSubtype.hashCode());
        result = prime * result + ref1;
        result = prime * result + ref2;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MockEventType other = (MockEventType) obj;
        if (duplicateHandling != other.duplicateHandling)
            return false;
        if (eventSubtype == null) {
            if (other.eventSubtype != null)
                return false;
        } else if (!eventSubtype.equals(other.eventSubtype))
            return false;
        if (ref1 != other.ref1)
            return false;
        if (ref2 != other.ref2)
            return false;
        return true;
    }
}
