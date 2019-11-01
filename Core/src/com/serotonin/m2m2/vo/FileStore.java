/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved. 
 *
 */
package com.serotonin.m2m2.vo;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;

/**
 *
 * @author Phillip Dunlap
 */
public class FileStore {
    private int id;
    private String storeName;
    private String readPermission;
    private String writePermission;
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
    public String getReadPermission() {
        return readPermission;
    }
    public void setReadPermission(String readPermission) {
        this.readPermission = readPermission;
    }
    public String getWritePermission() {
        return writePermission;
    }
    public void setWritePermission(String writePermission) {
        this.writePermission = writePermission;
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
        protected String getReadPermissions() {
            return fs.getReadPermission();
        }
        @Override
        protected String getWritePermissionTypeName() {
            return null;
        }
        @Override
        protected String getWritePermissions() {
            return fs.getWritePermission();
        }
    }
}
