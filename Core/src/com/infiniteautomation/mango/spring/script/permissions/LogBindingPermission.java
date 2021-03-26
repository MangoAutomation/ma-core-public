/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.PermissionGroup;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Allows access to a log file logger
 *
 * @author Jared Wiltshire
 */
public class LogBindingPermission extends PermissionDefinition {

    public static final String PERMISSION = "script.bindings.log";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("permission." + PERMISSION);
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
        return SCRIPTING_ENGINES_GROUP;
    }
}
