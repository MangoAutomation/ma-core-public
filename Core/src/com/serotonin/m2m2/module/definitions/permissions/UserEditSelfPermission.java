/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

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
    public Set<Set<Role>> getDefaultRoles() {
        Set<Set<Role>> roles = new HashSet<Set<Role>>();
        roles.add(Collections.singleton(PermissionHolder.USER_ROLE));
        return roles;
    }
}