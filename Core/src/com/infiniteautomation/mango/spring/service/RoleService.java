/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Terry Packer
 *
 */
@Service
public class RoleService extends AbstractVOService<RoleVO, RoleDao> {

    @Autowired
    public RoleService(RoleDao dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, RoleVO vo) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, RoleVO vo) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, RoleVO vo) {
        return Permissions.isValidPermissionHolder(user);
    }

    /**
     * Add a role to a permission
     * @param voId
     * @param role
     * @param permissionType
     * @param user
     */
    public void addRoleToVoPermission(RoleVO role, AbstractVO<?> vo, String permissionType, PermissionHolder user) {
        //TODO PermissionHolder check?
        // Superadmin ok
        // holder must contain the role already?
        this.dao.addRoleToVoPermission(role, vo, permissionType);
    }

    
    
}
