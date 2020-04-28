/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Define a file storage area within the filestore directory of the core
 *
 * @author Terry Packer
 */
public abstract class FileStoreDefinition extends ModuleElementDefinition {
    protected static final Log LOG = LogFactory.getLog(FileStoreDefinition.class);

    //Root directory within core
    public static final String ROOT = "filestore";
    public static final String FILE_STORE_LOCATION_ENV_PROPERTY = "filestore.location";

    /**
     * The translation for the name of the store
     * @return
     */
    abstract public TranslatableMessage getStoreDescription();

    /**
     * The name of the store.  Should be unique across all Modules and Mango Core
     *
     * @return the store name
     */
    abstract public String getStoreName();

    /**
     * Get the TypeName of the read permission definition, return null to allow access to all (including unauthenticated / public users)
     * @return
     */
    abstract protected String getReadPermissionTypeName();


    /**
     * Get the TypeName of the write permission definition, return null to allow access to all (including unauthenticated / public users)
     * @return
     */
    abstract protected String getWritePermissionTypeName();

    /**
     * Get the write permission
     * @return
     */
    public MangoPermission getWritePermission() {
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(getWritePermissionTypeName());
        return permission.getPermission();
    }

    /**
     * Get the read permission
     * @return
     */
    public MangoPermission getReadPermission() {
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(getReadPermissionTypeName());
        return permission.getPermission();
    }

    /**
     * Get the root of this filestore
     * @return
     * @throws IOException
     */
    public Path getRootPath() {
        String location = Common.envProps.getString(FILE_STORE_LOCATION_ENV_PROPERTY, ROOT);
        return Common.MA_HOME_PATH.resolve(location).resolve(getStoreName()).toAbsolutePath().normalize();
    }

    public File getRoot() {
        return getRootPath().toFile();
    }

    public void ensureExists() throws IOException {
        Files.createDirectories(getRootPath());
    }
}
