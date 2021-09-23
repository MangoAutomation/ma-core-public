/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import static com.infiniteautomation.mango.db.tables.SystemPermissions.SYSTEM_PERMISSIONS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.db.DatabaseProxy;

/**
 *
 * @author Terry Packer
 */
@Repository()
public class SystemPermissionDao extends BaseDao {

    private final PermissionService permissionService;

    @Autowired
    SystemPermissionDao(PermissionService permissionService, DatabaseProxy databaseProxy) {
        super(databaseProxy);
        this.permissionService = permissionService;
    }

    public MangoPermission get(String permissionType) {
        //TODO Mango 4.0 use a join
        Integer id = this.create.select(SYSTEM_PERMISSIONS.permissionId).from(SYSTEM_PERMISSIONS).where(SYSTEM_PERMISSIONS.permissionType.eq(permissionType)).limit(1).fetchOne(0, Integer.class);
        if(id != null) {
            return permissionService.get(id);
        }else {
            return null;
        }
    }

    /**
     * Update the permission
     * @param permissionTypeName
     * @param permission
     * @return
     */
    public void insert(String permissionTypeName, MangoPermission permission) {
        MangoPermission toInsert = permissionService.findOrCreate(permission);
        this.create.insertInto(SYSTEM_PERMISSIONS).columns(SYSTEM_PERMISSIONS.permissionId, SYSTEM_PERMISSIONS.permissionType).values(toInsert.getId(), permissionTypeName).execute();
    }

    /**
     * Update the permission
     * @param permissionTypeName
     * @param permission
     * @return
     */
    public void update(String permissionTypeName, MangoPermission existing, MangoPermission permission) {
        if(!existing.equals(permission)) {
            permissionService.deletePermissions(existing);
        }
        MangoPermission toUpdate = permissionService.findOrCreate(permission);
        this.create.update(SYSTEM_PERMISSIONS).set(SYSTEM_PERMISSIONS.permissionId, toUpdate.getId()).where(SYSTEM_PERMISSIONS.permissionType.eq(permissionTypeName)).execute();
    }

}
