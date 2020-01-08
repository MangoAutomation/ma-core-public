/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Phillip Dunlap
 */
public class FileStore extends AbstractBasicVO {
    private String storeName;
    private Set<Role> readRoles = Collections.emptySet();
    private Set<Role> writeRoles = Collections.emptySet();

    public String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Set<Role> getReadRoles() {
        return readRoles;
    }
    public void setReadRoles(Set<Role> readRoles) {
        this.readRoles = readRoles;
    }
    public Set<Role> getWriteRoles() {
        return writeRoles;
    }
    public void setWriteRoles(Set<Role> writeRoles) {
        this.writeRoles = writeRoles;
    }

    /**
     * Get the root of this filestore
     * @return
     * @throws IOException
     */
    public Path getRootPath() {
        String location = Common.envProps.getString(FileStoreDefinition.FILE_STORE_LOCATION_ENV_PROPERTY, FileStoreDefinition.ROOT);
        return Common.MA_HOME_PATH.resolve(location).resolve(getStoreName());
    }

}
