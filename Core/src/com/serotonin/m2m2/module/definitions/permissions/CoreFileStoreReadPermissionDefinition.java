/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
public class CoreFileStoreReadPermissionDefinition extends PermissionDefinition{

    public static final String TYPE_NAME = "filestore.core.read";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("filestore.core.permission.read");
    }

    @Override
    public String getPermissionTypeName() {
        return TYPE_NAME;
    }

    @Override
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE);
    }
}
