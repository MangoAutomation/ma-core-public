/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * Read access for internal status metrics
 *
 * @author Terry Packer
 */
public class SystemMetricsReadPermissionDefinition extends PermissionDefinition {

    public static final String PERMISSION = "internal.status";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("internal.metrics.permissions");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }
}
