/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreWritePermissionDefinition;

/**
 *
 * @author Terry Packer
 */
public class DocsFileStoreDefinition extends FileStoreDefinition{

    public static final String NAME = "docs";

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
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(DocsFileStoreReadPermissionDefinition.TYPE_NAME);
        return permission.getPermission();
    }

    @Override
    public MangoPermission getWritePermission() {
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(DocsFileStoreWritePermissionDefinition.TYPE_NAME);
        return permission.getPermission();
    }

}
