/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Permission to override view for any events, each individual event is also restricted
 *  based on the event type permission.
 *
 * @author Pier Puccini
 *
 */
public class EventsSuperadminViewPermissionDefinition extends PermissionDefinition {

    public static final String PERMISSION = "alarms.superadmin.view";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("alarms.permission.superadmin.view");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.requireAnyRole(PermissionHolder.SUPERADMIN_ROLE);
    }
}