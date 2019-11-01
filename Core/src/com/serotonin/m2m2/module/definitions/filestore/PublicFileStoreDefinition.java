/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PublicFileStoreWritePermissionDefinition;

/**
 * Access to the default public store
 * 
 * @author Jared Wiltshire
 */
public class PublicFileStoreDefinition extends FileStoreDefinition {

    public static final String NAME = "public";

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
    protected String getWritePermissionTypeName() {
        return PublicFileStoreWritePermissionDefinition.TYPE_NAME;
    }

}
