/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

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
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("systemSettings.permissions.superadmin");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.createOrSet(PermissionHolder.SUPERADMIN_ROLE);
    }

    @Override
    public void update(MangoPermission permission) {
        throw new ShouldNeverHappenException("Can't set superadmin roles");
    }
}
