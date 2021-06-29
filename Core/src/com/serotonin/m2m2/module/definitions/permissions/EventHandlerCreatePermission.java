/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * @author Terry Packer
 *
 */
public class EventHandlerCreatePermission extends PermissionDefinition {

    public static final String PERMISSION = "eventHandlers.create";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("eventHandlers.permission.create");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
