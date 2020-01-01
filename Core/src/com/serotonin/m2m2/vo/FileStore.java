/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved. 
 *
 */
package com.serotonin.m2m2.vo;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.i18n.TranslatableMessage;
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
    public FileStoreDefinition toDefinition() {
        return new UserFileStoreDefinition(this);
    }
    
    private class UserFileStoreDefinition extends FileStoreDefinition {
        final FileStore fs;
        UserFileStoreDefinition(FileStore fs) {
            this.fs = fs;
        }
        @Override
        public TranslatableMessage getStoreDescription() {
            return new TranslatableMessage("filestore.user.description", fs.getStoreName());
        }
        @Override
        public String getStoreName() {
            return fs.getStoreName();
        }
        @Override
        protected String getReadPermissionTypeName() {
            return null;
        }
        @Override
        protected Set<Role> getReadRoles() {
            return fs.getReadRoles();
        }
        @Override
        protected String getWritePermissionTypeName() {
            return null;
        }
        @Override
        protected Set<Role> getWriteRoles() {
            return fs.getWriteRoles();
        }
    }
}
