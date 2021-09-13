/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Permission to view any data point.
 *
 * @author Benjamin Perez
 *
 */
public class DataPointPermissionDefinition extends PermissionDefinition {
    public static final String PERMISSION = "user.view.dataPoint";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("users.permissions.view.dataPoint");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.requireAnyRole(PermissionHolder.SUPERADMIN_ROLE);
    }

}
