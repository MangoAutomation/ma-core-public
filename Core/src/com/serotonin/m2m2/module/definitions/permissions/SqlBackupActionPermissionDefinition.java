/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class SqlBackupActionPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "action.sqlBackup";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("systemSettings.databaseBackupSettings");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
