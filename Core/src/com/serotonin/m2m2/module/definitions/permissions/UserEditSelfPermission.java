/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * This permission determines who can edit themselves
 *
 * @author Terry Packer
 *
 */
public class UserEditSelfPermission extends PermissionDefinition{
    public static final String PERMISSION = "permissions.user.editSelf";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("systemSettings.permissions.userEditSelf");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE);
    }
}