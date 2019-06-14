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
public class PurgeWithPurgeSettingsActionPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "action.purgeUsingSettings";

    @Override
    public String getPermissionKey() {
        return "systemSettings.purgeNow";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
