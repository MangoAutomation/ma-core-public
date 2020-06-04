/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * TODO Mango 4.0 no longer used? Should we use this in the UI to hide/show the users menu item?
 *
 * Permission to view the users page
 *
 * @author Terry Packer
 *
 */
public class UsersViewPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "users.view";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("users.permissions.view");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.createOrSet(PermissionHolder.USER_ROLE);
    }
}
