/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class PurgeAllPointValuesActionPermissionDefinition extends PermissionDefinition {

    public static final String PERMISSION = "action.purgeAllPointValues";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("systemSettings.purgeData");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
