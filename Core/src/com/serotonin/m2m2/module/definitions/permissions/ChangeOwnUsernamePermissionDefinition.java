/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
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
    public Set<Set<Role>> getDefaultRoles() {
        Set<Set<Role>> roles = new HashSet<Set<Role>>();
        roles.add(Collections.singleton(PermissionHolder.USER_ROLE));
        return roles;
    }

}
