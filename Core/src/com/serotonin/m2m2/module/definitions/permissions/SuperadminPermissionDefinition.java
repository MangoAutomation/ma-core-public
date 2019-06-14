/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.List;

import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.Permission;

/**
 * @author Terry Packer
 *
 */
public class SuperadminPermissionDefinition extends PermissionDefinition {
    public static final String GROUP_NAME = "superadmin";
    public static final String PERMISSION = "permissions.superadmin";

    public SuperadminPermissionDefinition() {
    }

    @Override
    public String getPermissionKey() {
        return "systemSettings.permissions.superadmin";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    public List<String> getDefaultGroups() {
        return Collections.singletonList(GROUP_NAME);
    }

    @Override
    public Permission getPermission() {
        return new Permission(PERMISSION, Collections.singleton(GROUP_NAME));
    }
}
