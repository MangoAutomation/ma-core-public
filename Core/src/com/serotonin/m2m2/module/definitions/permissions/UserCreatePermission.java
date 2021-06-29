/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.PermissionGroup;

/**
 * @author Terry Packer
 *
 */
public class UserCreatePermission extends PermissionDefinition {

    public static final String PERMISSION = "users.create";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("users.permissions.create");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    public PermissionGroup getGroup() {
        return USERS_GROUP;
    }
}
