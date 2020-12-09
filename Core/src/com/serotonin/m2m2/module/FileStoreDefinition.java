/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.FileStore;

/**
 * Define a file storage area within the filestore directory of the core
 *
 * @author Terry Packer
 */
public abstract class FileStoreDefinition extends ModuleElementDefinition {
    protected static final Log LOG = LogFactory.getLog(FileStoreDefinition.class);

    //Root directory within core
    private static final Path FILE_STORE_PATH = Common.getFileStorePath();

    /**
     * The translation for the name of the store
     * @return
     */
    abstract public TranslatableMessage getStoreDescription();

    /**
     * The name of the store.  Should be unique across all Modules and Mango Core.
     * Note: this is used as the pseudo-XID when this file store definition is retrieved from the REST API.
     *
     * @return the store name
     */
    abstract public String getStoreName();

    /**
     * Get the write permission, should delegate to a {@link PermissionDefinition}
     * @return
     */
    public abstract MangoPermission getWritePermission();

    /**
     * Get the read permission, should delegate to a {@link PermissionDefinition}
     * @return
     */
    public abstract MangoPermission getReadPermission();

    /**
     * Get the root of this filestore
     * @return
     */
    public Path getRootPath() {
        return FILE_STORE_PATH.resolve(getStoreName()).normalize();
    }

    public void ensureExists() throws IOException {
        Files.createDirectories(getRootPath());
    }

    public FileStore toFileStore() {
        FileStore fileStore = new FileStore();
        fileStore.setBuiltIn(true);
        fileStore.setXid(getStoreName());
        // TODO maybe use current user locale?
        fileStore.setName(getStoreDescription().translate(Common.getTranslations()));
        fileStore.setWritePermission(getWritePermission());
        fileStore.setReadPermission(getReadPermission());
        return fileStore;
    }
}
