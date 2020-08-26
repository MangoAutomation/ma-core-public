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
import com.serotonin.m2m2.module.definitions.permissions.PublicFileStoreWritePermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Access to the default public store
 *
 * @author Jared Wiltshire
 */
public class PublicFileStoreDefinition extends FileStoreDefinition {

    public static final String NAME = "public";

    private final MangoPermission readPermission;

    public PublicFileStoreDefinition() {
        this.readPermission = MangoPermission.requireAnyRole(PermissionHolder.ANONYMOUS_ROLE, PermissionHolder.USER_ROLE);
    }

    @Override
    public TranslatableMessage getStoreDescription() {
        return new TranslatableMessage("filestore.public.description");
    }

    @Override
    public String getStoreName() {
        return NAME;
    }

    @Override
    public MangoPermission getReadPermission() {
        return readPermission;
    }

    @Override
    public MangoPermission getWritePermission() {
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(PublicFileStoreWritePermissionDefinition.TYPE_NAME);
        return permission.getPermission();
    }

}
