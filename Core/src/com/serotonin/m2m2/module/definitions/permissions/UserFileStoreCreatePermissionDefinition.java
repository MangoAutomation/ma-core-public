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
public class UserFileStoreCreatePermissionDefinition extends PermissionDefinition {

    public static final String TYPE_NAME = "filestore.user.create";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("filestore.user.permission.create");
    }

    @Override
    public String getPermissionTypeName() {
        return TYPE_NAME;
    }

}
