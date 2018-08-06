/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type;

import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;

/**
 *
 * @author Terry Packer
 */
public class MockEventType extends EventType {

    public static final String TYPE_NAME = "MOCK";
    private int duplicateHandling;
    private String eventSubtype;
    private int ref1;
    private int ref2;

    /**
     * Create a mock event type with duplicate handling of ALLOW
     * and event sub type of null
     */
    public MockEventType() {
        this(EventType.DuplicateHandling.ALLOW);
    }

    /**
     * default subtype of null
     * @param duplicateHandling
     */
    public MockEventType(int duplicateHandling) {
        this(duplicateHandling, null);
    }

    public MockEventType(int duplicateHandling, String eventSubType) {
        this(duplicateHandling, eventSubType, -1, -1);
    }

    public MockEventType(int duplicateHandling, String eventSubType, int ref1, int ref2) {
        this.duplicateHandling = duplicateHandling;
        this.eventSubtype = null;
        this.ref1 = ref1;
        this.ref2 = ref2;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getEventType()
     */
    @Override
    public String getEventType() {
        return TYPE_NAME;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getEventSubtype()
     */
    @Override
    public String getEventSubtype() {
        return eventSubtype;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getDuplicateHandling()
     */
    @Override
    public int getDuplicateHandling() {
        return duplicateHandling;
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#asModel()
     */
    @Override
    public EventTypeModel asModel() {
        return null;
    }

    @Override
    public boolean hasPermission(PermissionHolder user) {
        return true;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getDataPointId()
     */
    @Override
    public int getDataPointId() {
        return ref2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + duplicateHandling;
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
