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
public class ConfigurationBackupActionPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "action.configurationBackup";

    @Override
    public String getPermissionKey() {
        return "systemSettings.backupSettings";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
