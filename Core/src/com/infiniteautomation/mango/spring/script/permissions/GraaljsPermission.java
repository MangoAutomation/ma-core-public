/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.permissions;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Grants access to use the Graal.js scripting engine
 * @author Jared Wiltshire
 */
public class GraaljsPermission extends PermissionDefinition {

    public static final String PERMISSION = "script.graaljs";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("permission." + PERMISSION);
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    protected Set<Set<Role>> getDefaultRoles() {
        return Collections.singleton(Collections.singleton(PermissionHolder.USER_ROLE));
    }

}
