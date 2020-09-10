/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * Allows access to setTimeout() and setInterval() functions
 *
 * @author Jared Wiltshire
 */
public class TimeoutAndIntervalPermission extends PermissionDefinition {

    public static final String PERMISSION = "script.bindings.timeoutAndInterval";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("permission." + PERMISSION);
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.superadminOnly();
    }
}
