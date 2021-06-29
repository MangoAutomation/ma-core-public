/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
public class MockEventTypeDefinition extends EventTypeDefinition {

    @Override
    public String getTypeName() {
        return MockEventType.TYPE_NAME;
    }

    @Override
    public Class<? extends EventType> getEventTypeClass() {
        return MockEventType.class;
    }

    @Override
    public EventType createEventType(String subtype, int ref1, int ref2) {
        return new MockEventType(DuplicateHandling.ALLOW, subtype, ref1, ref2, null);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, PermissionService service) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<EventTypeVO> getEventTypeVOs(PermissionHolder user, PermissionService service) {
        //TODO implement when necessary
        return new ArrayList<EventTypeVO>();
    }

    @Override
    public String getDescriptionKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TranslatableMessage getSourceDisabledMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getEventSubTypes(PermissionHolder user, PermissionService service) {
        return Collections.emptyList();
    }

    @Override
    public boolean supportsReferenceId1() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsReferenceId2() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public EventTypeVO createDefaultEventTypeVO(String subtype, int ref1, int ref2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean supportsSubType() {
        // TODO Auto-generated method stub
        return false;
    }
}
