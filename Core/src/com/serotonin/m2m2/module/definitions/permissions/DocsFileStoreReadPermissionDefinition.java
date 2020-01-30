/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class DocsFileStoreReadPermissionDefinition extends PermissionDefinition{

    public static final String TYPE_NAME = "filestore.docs.read";

    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("filestore.docs.permission.read");
    }

    @Override
    public String getPermissionTypeName() {
        return TYPE_NAME;
    }

    @Override
    public Set<Role> getDefaultRoles() {
        return Collections.singleton(PermissionHolder.USER_ROLE);
    }
}
