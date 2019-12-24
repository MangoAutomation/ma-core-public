/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * @author Terry Packer
 *
 */
public abstract class ServiceWithPermissionsTestBase<VO extends AbstractVO<?>, DAO extends AbstractDao<VO>, SERVICE extends AbstractVOService<VO,DAO>> extends ServiceTestBase<VO, DAO, SERVICE> {

    /**
     * The type name for the create permission of the VO
     * @return
     */
    abstract String getCreatePermissionType();
    abstract void setReadRoles(Set<RoleVO> roles, VO vo);
    abstract void setEditRoles(Set<RoleVO> roles, VO vo);
    
    @Test(expected = PermissionException.class)
    public void testCreatePrivilegeFails() {
        VO vo = newVO();
        service.insertFull(vo, editUser);
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            VO vo = newVO();
            addRoleToCreatePermission(editRole);
            setReadRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            service.insertFull(vo, editUser);
        });
    }
    
    @Test
    public void testUserReadRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testUserReadRoleFails() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.emptySet(), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
        });
    }
    
    @Test
    public void testUserEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            setEditRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            service.updateFull(fromDb.getXid(), fromDb, readUser);
            VO updated = service.getFull(fromDb.getId(), readUser);
            assertVoEqual(fromDb, updated);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            setEditRoles(Collections.emptySet(), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            service.updateFull(fromDb.getXid(), fromDb, readUser);
            VO updated = service.getFull(fromDb.getId(), readUser);
            assertVoEqual(fromDb, updated);
        });
    }
    
    @Test
    public void testUserDelete() {
        runTest(() -> {
            VO vo = newVO();
            addRoleToCreatePermission(editRole);
            setReadRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            setEditRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            vo = service.insertFull(vo, editUser);
            service.delete(vo.getId(), editUser);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testUserDeleteFails() {
        runTest(() -> {
            VO vo = newVO();
            service.insertFull(vo, systemSuperadmin);
            service.delete(vo.getId(), editUser);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testSuperadminReadRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(RoleDao.getInstance().getSuperadminRole()), vo);
            service.insertFull(vo, systemSuperadmin);
            service.getFull(vo.getId(), readUser);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testSuperadminEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(RoleDao.getInstance().getUserRole()), vo);
            setEditRoles(Collections.singleton(RoleDao.getInstance().getSuperadminRole()), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            service.updateFull(fromDb.getXid(), fromDb, readUser);            
        });
    }
    
    void addRoleToCreatePermission(RoleVO vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            roleService.addRoleToPermission(vo, getCreatePermissionType(), systemSuperadmin);
        }
    }
}
