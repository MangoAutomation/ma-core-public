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
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreWritePermissionDefinition;

/**
 * Access to the default core store
 *
 * @author Terry Packer
 */
public class CoreFileStoreDefinition extends FileStoreDefinition{

    public static final String NAME = "default";

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
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(CoreFileStoreReadPermissionDefinition.TYPE_NAME);
        return permission.getPermission();
    }

    @Override
    public MangoPermission getWritePermission() {
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(CoreFileStoreWritePermissionDefinition.TYPE_NAME);
        return permission.getPermission();
    }

}
