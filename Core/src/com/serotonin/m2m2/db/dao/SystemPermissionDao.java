/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.tables.SystemPermissionTable.SYSTEM_PERMISSIONS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.permission.MangoPermission;
/**
 *
 * @author Terry Packer
 */
@Repository()
public class SystemPermissionDao extends BaseDao {

    private final PermissionDao permissionDao;

    @Autowired
    SystemPermissionDao(PermissionDao permissionDao) {
        this.permissionDao = permissionDao;
    }

    public MangoPermission get(String permissionType) {
        //TODO Mango 4.0 use a join
        Integer id = this.create.select(SYSTEM_PERMISSIONS.permissionId).from(SYSTEM_PERMISSIONS).where(SYSTEM_PERMISSIONS.permissionType.eq(permissionType)).limit(1).fetchOne(0, Integer.class);
        if(id != null) {
            return permissionDao.get(id);
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
        permissionDao.permissionId(permission);
        this.create.insertInto(SYSTEM_PERMISSIONS).columns(SYSTEM_PERMISSIONS.permissionId, SYSTEM_PERMISSIONS.permissionType).values(permission.getId(), permissionTypeName).execute();
    }

    /**
     * Update the permission
     * @param permissionTypeName
     * @param permission
     * @return
     */
    public void update(String permissionTypeName, MangoPermission existing, MangoPermission permission) {
        permissionDao.permissionId(permission);
        if(!existing.equals(permission)) {
            permissionDao.permissionDeleted(existing);
        }
        this.create.update(SYSTEM_PERMISSIONS).set(SYSTEM_PERMISSIONS.permissionId, permission.getId()).where(SYSTEM_PERMISSIONS.permissionType.eq(permissionTypeName)).execute();
    }

}
