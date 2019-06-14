/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.List;

import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * This permission determines who can edit themselves
 *
 * @author Terry Packer
 *
 */
public class UserEditSelfPermission extends PermissionDefinition{
    public static final String PERMISSION = "permissions.user.editSelf";

    @Override
    public String getPermissionKey() {
        return "systemSettings.permissions.userEditSelf";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    public List<String> getDefaultGroups() {
        return Collections.singletonList(Permissions.USER_DEFAULT);
    }
}