/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.RoleVO;

/**
 * This permission determines who can edit themselves
 *
 * @author Terry Packer
 *
 */
public class UserEditSelfPermission extends PermissionDefinition{
    public static final String PERMISSION = "permissions.user.editSelf";

    @Override
    public String getPermissionKey() {
        return "systemSettings.permissions.userEditSelf";
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