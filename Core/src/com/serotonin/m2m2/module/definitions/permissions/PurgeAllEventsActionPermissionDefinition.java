/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.module.PermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class PurgeAllEventsActionPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "action.purgeAllEvents";

    @Override
    public String getPermissionKey() {
        return "systemSettings.purgeAllEvents";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
