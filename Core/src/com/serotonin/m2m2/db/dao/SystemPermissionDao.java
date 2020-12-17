/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;

import static com.infiniteautomation.mango.db.tables.SystemPermissions.SYSTEMPERMISSIONS;
/**
 *
 * @author Terry Packer
 */
@Repository()
public class SystemPermissionDao extends BaseDao {

    private final PermissionService permissionService;

    @Autowired
    SystemPermissionDao(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public MangoPermission get(String permissionType) {
        //TODO Mango 4.0 use a join
        Integer id = this.create.select(SYSTEMPERMISSIONS.permissionId).from(SYSTEMPERMISSIONS).where(SYSTEMPERMISSIONS.permissionType.eq(permissionType)).limit(1).fetchOne(0, Integer.class);
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
        this.create.insertInto(SYSTEMPERMISSIONS).columns(SYSTEMPERMISSIONS.permissionId, SYSTEMPERMISSIONS.permissionType).values(toInsert.getId(), permissionTypeName).execute();
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
        this.create.update(SYSTEMPERMISSIONS).set(SYSTEMPERMISSIONS.permissionId, toUpdate.getId()).where(SYSTEMPERMISSIONS.permissionType.eq(permissionTypeName)).execute();
    }

}
