/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Permission to view the users page
 * 
 * @author Terry Packer
 *
 */
public class UsersViewPermissionDefinition extends PermissionDefinition{

    public static final String PERMISSION = "users.view";

    @Override
    public String getPermissionKey() {
        return "users.permissions.view";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }
    
    @Override
    public Set<RoleVO> getDefaultRoles() {
        return Collections.singleton(RoleDao.getInstance().getUserRole());
    }
}
