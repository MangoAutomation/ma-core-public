/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicVOServiceWithPermissionsTestBase<VO extends AbstractBasicVO, DAO extends AbstractBasicDao<VO>, SERVICE extends AbstractBasicVOService<VO,DAO>> extends AbstractBasicVOServiceTest<VO, DAO, SERVICE> {

    public AbstractBasicVOServiceWithPermissionsTestBase() {
        
    }
    
    public AbstractBasicVOServiceWithPermissionsTestBase(boolean enableWebDb, int webDbPort) {
        super(enableWebDb, webDbPort);
    }
    
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
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            service.insertFull(vo, editUser);
        });
    }
    
    @Test
    public void testUserReadRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
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
    
    @Test(expected = ValidationException.class)
    public void testReadRolesCannotBeNull() {
        VO vo = newVO();
        setReadRoles(null, vo);
        service.insertFull(vo, systemSuperadmin);
    }
    
    @Test(expected = ValidationException.class)
    public void testCannotRemoveReadAccess() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insertFull(vo, systemSuperadmin);
        VO fromDb = service.getFull(vo.getId(), readUser);
        assertVoEqual(vo, fromDb);
        setReadRoles(Collections.emptySet(), fromDb);
        service.updateFull(fromDb.getId(), fromDb, readUser);
    }
    
    @Test(expected = ValidationException.class)
    public void testAddReadRoleUserDoesNotHave() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insertFull(vo, systemSuperadmin);
        VO fromDb = service.getFull(vo.getId(), readUser);
        assertVoEqual(vo, fromDb);
        setReadRoles(Collections.singleton(roleService.getSuperadminRole()), fromDb);
        service.updateFull(fromDb.getId(), fromDb, readUser);
    }
    
    @Test
    public void testUserEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
            service.updateFull(fromDb.getId(), fromDb, readUser);
            VO updated = service.getFull(fromDb.getId(), readUser);
            assertVoEqual(fromDb, updated);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.emptySet(), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
            service.updateFull(fromDb.getId(), fromDb, readUser);
            VO updated = service.getFull(fromDb.getId(), readUser);
            assertVoEqual(fromDb, updated);
        });
    }
    
    @Test(expected = ValidationException.class)
    public void testEditRolesCannotBeNull() {
        VO vo = newVO();
        setEditRoles(null, vo);
        service.insertFull(vo, systemSuperadmin);
    }
    
    @Test(expected = ValidationException.class)
    public void testCannotRemoveEditAccess() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insertFull(vo, systemSuperadmin);
        VO fromDb = service.getFull(vo.getId(), readUser);
        assertVoEqual(vo, fromDb);
        setEditRoles(Collections.emptySet(), fromDb);
        service.updateFull(fromDb.getId(), fromDb, readUser);
    }
    
    @Test(expected = ValidationException.class)
    public void testAddEditRoleUserDoesNotHave() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insertFull(vo, systemSuperadmin);
        VO fromDb = service.getFull(vo.getId(), readUser);
        assertVoEqual(vo, fromDb);
        setEditRoles(Collections.singleton(roleService.getSuperadminRole()), fromDb);
        service.updateFull(fromDb.getId(), fromDb, readUser);
    }
    
    @Test
    public void testUserCanDelete() {
        runTest(() -> {
            VO vo = newVO();
            addRoleToCreatePermission(editRole);
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
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
            setReadRoles(Collections.singleton(roleService.getSuperadminRole()), vo);
            service.insertFull(vo, systemSuperadmin);
            service.getFull(vo.getId(), readUser);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testSuperadminEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getSuperadminRole()), vo);
            service.insertFull(vo, systemSuperadmin);
            VO fromDb = service.getFull(vo.getId(), readUser);
            assertVoEqual(vo, fromDb);
            service.updateFull(fromDb.getId(), fromDb, readUser);            
        });
    }
    
    
    
    void addRoleToCreatePermission(RoleVO vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            roleService.addRoleToPermission(vo, getCreatePermissionType(), systemSuperadmin);
        }
    }
}
