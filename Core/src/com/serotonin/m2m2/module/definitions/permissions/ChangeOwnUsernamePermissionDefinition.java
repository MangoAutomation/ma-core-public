/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.PermissionGroup;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Permission to allow changing ones own username
 * @author Terry Packer
 */
public class ChangeOwnUsernamePermissionDefinition extends PermissionDefinition{
    public static final String PERMISSION = "permissions.user.changeUsername";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("systemSettings.permissions.changeUsername");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE);
    }

    @Override
    public PermissionGroup getGroup() {
        return USERS_GROUP;
    }
}
