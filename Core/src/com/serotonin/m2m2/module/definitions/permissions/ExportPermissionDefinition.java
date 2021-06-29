/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

public class ExportPermissionDefinition extends PermissionDefinition {
    public static final String PERMISSION = "config.export";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("permission.config.export");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }
}
