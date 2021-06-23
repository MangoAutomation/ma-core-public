/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.PermissionGroup;

/**
 *
 * Permission to view the users page, only used in front end. Doesn't control access to user profile page.
 *
 * @author Terry Packer
 * @author Jared Wiltshire
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
    public PermissionGroup getGroup() {
        return USERS_GROUP;
    }
}
