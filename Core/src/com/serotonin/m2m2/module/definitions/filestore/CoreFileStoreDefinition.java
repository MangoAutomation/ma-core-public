/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreWritePermissionDefinition;

/**
 * Access to the default core store
 *
 * @author Terry Packer
 */
public class CoreFileStoreDefinition extends FileStoreDefinition{

    public static final String NAME = "default";

    @Autowired
    private CoreFileStoreReadPermissionDefinition readPermission;
    @Autowired
    private CoreFileStoreWritePermissionDefinition writePermission;

    @Override
    public TranslatableMessage getStoreDescription() {
        return new TranslatableMessage("filestore.core.description");
    }

    @Override
    public String getStoreName() {
        return NAME;
    }

    @Override
    public MangoPermission getReadPermission() {
        return readPermission.getPermission();
    }

    @Override
    public MangoPermission getWritePermission() {
        return writePermission.getPermission();
    }

}
