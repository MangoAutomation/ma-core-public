/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.RoleVO;

/**
 *
 * @author Terry Packer
 */
public class CoreFileStoreReadPermissionDefinition extends PermissionDefinition{

    public static final String TYPE_NAME = "filestore.core.read";

    @Override
    public String getPermissionKey() {
        return "filestore.core.permission.read";
    }

    @Override
    public String getPermissionTypeName() {
        return TYPE_NAME;
    }

    @Override
    public Set<RoleVO> getDefaultRoles() {
        return Collections.singleton(RoleDao.getInstance().getUserRole());
    }
}
