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
public class SqlBackupActionPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "action.sqlBackup";

    @Override
    public String getPermissionKey() {
        return "systemSettings.databaseBackupSettings";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
