/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.List;

import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 *
 * @author Terry Packer
 */
public class DocsFileStoreReadPermissionDefinition extends PermissionDefinition{

    public static final String TYPE_NAME = "filestore.docs.read";

    @Override
    public String getPermissionKey() {
        return "filestore.docs.permission.read";
    }

    @Override
    public String getPermissionTypeName() {
        return TYPE_NAME;
    }

    @Override
    public List<String> getDefaultGroups() {
        return Collections.singletonList(Permissions.USER_DEFAULT);
    }
}
