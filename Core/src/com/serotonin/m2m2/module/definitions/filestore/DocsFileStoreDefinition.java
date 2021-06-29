/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.filestore;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreWritePermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class DocsFileStoreDefinition extends FileStoreDefinition{

    public static final String NAME = "docs";

    @Autowired
    private DocsFileStoreReadPermissionDefinition readPermission;
    @Autowired
    private DocsFileStoreWritePermissionDefinition writePermission;

    @Override
    public TranslatableMessage getStoreDescription() {
        return new TranslatableMessage("filestore.docs.description");
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
