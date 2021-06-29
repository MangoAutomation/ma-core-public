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
public class PurgeAllEventsActionPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "action.purgeAllEvents";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("systemSettings.purgeAllEvents");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
