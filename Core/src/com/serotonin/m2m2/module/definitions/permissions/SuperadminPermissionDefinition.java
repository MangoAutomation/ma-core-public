/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.Collections;
import java.util.Set;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public class SuperadminPermissionDefinition extends PermissionDefinition {
    public static final String GROUP_NAME = "superadmin";
    public static final String PERMISSION = "permissions.superadmin";

    private final LazyInitSupplier<MangoPermission> permission = new LazyInitSupplier<>(() -> {
        return new MangoPermission(PERMISSION, Collections.singleton(RoleDao.getInstance().getSuperadminRole()));
    });
    
    public SuperadminPermissionDefinition() {
    
    }

    @Override
    public String getPermissionKey() {
        return "systemSettings.permissions.superadmin";
    }

    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }

    @Override
    public Set<RoleVO> getDefaultRoles() {
        return Collections.singleton(RoleDao.getInstance().getSuperadminRole());
    }

    @Override
    public MangoPermission getPermission() {
        return permission.get();
    }
}
