/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * This permission determines if a User can register other users as disabled users
 *
 * @author Terry Packer
 *
 */
public class SendToMailingListPermission extends PermissionDefinition {
    public static final String PERMISSION = "permissions.user.sendToMailingList";

    @Override
    public String getPermissionKey() {
        return "systemSettings.permissions.sendToMailingList";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }
}