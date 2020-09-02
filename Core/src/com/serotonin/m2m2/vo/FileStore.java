/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.nio.file.Path;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.FileStoreDefinition;

/**
 *
 * @author Phillip Dunlap
 */
public class FileStore extends AbstractBasicVO {
    private boolean fromDefinition = false;
    private String storeName;
    private MangoPermission readPermission = new MangoPermission();
    private MangoPermission writePermission = new MangoPermission();

    public String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public MangoPermission getReadPermission() {
        return readPermission;
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission = readPermission;
    }

    public MangoPermission getWritePermission() {
        return writePermission;
    }

    public void setWritePermission(MangoPermission writePermission) {
        this.writePermission = writePermission;
    }

    public boolean isFromDefinition() {
        return fromDefinition;
    }

    public void setFromDefinition(boolean fromDefinition) {
        this.fromDefinition = fromDefinition;
    }

    /**
     * Get the root of this filestore
     * @return
     * @throws IOException
     */
    public Path getRootPath() {
        String location = Common.envProps.getString(FileStoreDefinition.FILE_STORE_LOCATION_ENV_PROPERTY, FileStoreDefinition.ROOT);
        return Common.MA_HOME_PATH.resolve(location).resolve(getStoreName()).toAbsolutePath().normalize();
    }

}
