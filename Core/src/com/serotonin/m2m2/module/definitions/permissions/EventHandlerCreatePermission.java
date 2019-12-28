/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * @author Terry Packer
 *
 */
public class EventHandlerCreatePermission extends PermissionDefinition {

    public static final String PERMISSION = "eventHandlers.create";

    @Override
    public String getPermissionKey() {
        return "eventHandlers.permission.create";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
