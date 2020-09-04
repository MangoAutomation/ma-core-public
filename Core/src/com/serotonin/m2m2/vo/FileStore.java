/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.vo;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;

/**
 * @author Phillip Dunlap
 * @author Jared Wiltshire
 */
public class FileStore extends AbstractVO {
    public static final String XID_PREFIX = "FS_";

    private boolean builtIn = false;

    @JsonProperty
    private MangoPermission readPermission = new MangoPermission();
    @JsonProperty
    private MangoPermission writePermission = new MangoPermission();

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

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    @Override
    public String getTypeKey() {
        return FileStoreAuditEvent.TYPE_KEY;
    }

    public static class FileStoreAuditEvent extends AuditEventTypeDefinition {

        public static final String TYPE_NAME = "FILE_STORE";
        public static final String TYPE_KEY = "filestore.description";

        @Override
        public String getTypeName() {
            return TYPE_NAME;
        }

        @Override
        public String getDescriptionKey() {
            return TYPE_KEY;
        }

    }

}
