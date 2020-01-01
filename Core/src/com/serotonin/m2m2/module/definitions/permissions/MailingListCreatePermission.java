/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * @author Terry Packer
 *
 */
public class MailingListCreatePermission extends PermissionDefinition {

    public static final String PERMISSION = "mailingLists.create";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("mailingLists.permission.create");
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

}
