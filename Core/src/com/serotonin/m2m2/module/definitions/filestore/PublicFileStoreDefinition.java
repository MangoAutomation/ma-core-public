/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
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
        //TODO Mango 4.0 should change to new 'public' role
        this.readPermission = MangoPermission.createAndSet(PermissionHolder.USER_ROLE);
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
    protected String getReadPermissionTypeName() {
        return null;
    }

    @Override
    public MangoPermission getReadPermission() {
        return readPermission;
    }

    @Override
    protected String getWritePermissionTypeName() {
        return PublicFileStoreWritePermissionDefinition.TYPE_NAME;
    }

}
