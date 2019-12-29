/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractVOServiceWithPermissionsTest<VO extends AbstractVO<VO>, DAO extends AbstractDao<VO>, SERVICE extends AbstractVOService<VO,DAO>> extends AbstractVOServiceTest<VO, DAO, SERVICE> {

    public AbstractVOServiceWithPermissionsTest() {
        
    }
    
    public AbstractVOServiceWithPermissionsTest(boolean enableWebDb, int webDbPort) {
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
        service.insert(vo, true, editUser);
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            VO vo = newVO();
            addRoleToCreatePermission(editRole);
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            service.insert(vo, true, editUser);
        });
    }
    
    @Test
    public void testUserReadRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            service.insert(vo, true, systemSuperadmin);
            VO fromDb = service.get(vo.getId(), true, readUser);
            assertVoEqual(vo, fromDb);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testUserReadRoleFails() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.emptySet(), vo);
            service.insert(vo, true, systemSuperadmin);
            VO fromDb = service.get(vo.getId(), true, readUser);
            assertVoEqual(vo, fromDb);
        });
    }
    
    @Test(expected = ValidationException.class)
    public void testReadRolesCannotBeNull() {
        VO vo = newVO();
        setReadRoles(null, vo);
        service.insert(vo, true, systemSuperadmin);
    }
    
    @Test(expected = ValidationException.class)
    public void testCannotRemoveReadAccess() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insert(vo, true, systemSuperadmin);
        VO fromDb = service.get(vo.getId(), true, readUser);
        assertVoEqual(vo, fromDb);
        fromDb.setName("read user edited me");
        setReadRoles(Collections.emptySet(), fromDb);
        service.update(fromDb.getXid(), fromDb, true, readUser);
    }
    
    @Test(expected = ValidationException.class)
    public void testAddReadRoleUserDoesNotHave() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insert(vo, true, systemSuperadmin);
        VO fromDb = service.get(vo.getId(), true, readUser);
        assertVoEqual(vo, fromDb);
        fromDb.setName("read user edited me");
        setReadRoles(Collections.singleton(roleService.getSuperadminRole()), fromDb);
        service.update(fromDb.getXid(), fromDb, true, readUser);
    }
    
    @Test
    public void testUserEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            service.insert(vo, true, systemSuperadmin);
            VO fromDb = service.get(vo.getId(), true, readUser);
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            service.update(fromDb.getXid(), fromDb, true, readUser);
            VO updated = service.get(fromDb.getId(),true,  readUser);
            assertVoEqual(fromDb, updated);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.emptySet(), vo);
            service.insert(vo, true, systemSuperadmin);
            VO fromDb = service.get(vo.getId(), true, readUser);
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            service.update(fromDb.getXid(), fromDb, true, readUser);
            VO updated = service.get(fromDb.getId(), true, readUser);
            assertVoEqual(fromDb, updated);
        });
    }
    
    @Test(expected = ValidationException.class)
    public void testEditRolesCannotBeNull() {
        VO vo = newVO();
        setEditRoles(null, vo);
        service.insert(vo, true, systemSuperadmin);
    }
    
    @Test(expected = ValidationException.class)
    public void testCannotRemoveEditAccess() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insert(vo, true, systemSuperadmin);
        VO fromDb = service.get(vo.getId(), true, readUser);
        assertVoEqual(vo, fromDb);
        fromDb.setName("read user edited me");
        setEditRoles(Collections.emptySet(), fromDb);
        service.update(fromDb.getXid(), fromDb, true, readUser);
    }
    
    @Test(expected = ValidationException.class)
    public void testAddEditRoleUserDoesNotHave() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        service.insert(vo, true, systemSuperadmin);
        VO fromDb = service.get(vo.getId(), true, readUser);
        assertVoEqual(vo, fromDb);
        fromDb.setName("read user edited me");
        setEditRoles(Collections.singleton(roleService.getSuperadminRole()), fromDb);
        service.update(fromDb.getXid(), fromDb, true, readUser);
    }
    
    @Test
    public void testUserCanDelete() {
        runTest(() -> {
            VO vo = newVO();
            addRoleToCreatePermission(editRole);
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            vo = service.insert(vo, true, editUser);
            service.delete(vo.getId(), editUser);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testUserDeleteFails() {
        runTest(() -> {
            VO vo = newVO();
            service.insert(vo, true, systemSuperadmin);
            service.delete(vo.getId(), editUser);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testSuperadminReadRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getSuperadminRole()), vo);
            service.insert(vo, true, systemSuperadmin);
            service.get(vo.getId(), true, readUser);
        });
    }
    
    @Test(expected = PermissionException.class)
    public void testSuperadminEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getSuperadminRole()), vo);
            service.insert(vo, true, systemSuperadmin);
            VO fromDb = service.get(vo.getId(), true, readUser);
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            service.update(fromDb.getXid(), fromDb, true, readUser);            
        });
    }
    
    @Test
    public void testDeleteRoleUpdateVO() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(readRole), vo);
            setEditRoles(Collections.singleton(editRole), vo);
            service.insert(vo, true, systemSuperadmin);
            VO fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            roleService.delete(editRole, systemSuperadmin);
            roleService.delete(readRole, systemSuperadmin);
            VO updated = service.get(fromDb.getId(),true,  systemSuperadmin);
            setReadRoles(Collections.emptySet(), fromDb);
            setEditRoles(Collections.emptySet(), fromDb);
            assertVoEqual(fromDb, updated);
        });
    }
    
    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            VO vo = insertNewVO();
            setReadRoles(Collections.singleton(readRole), vo);
            setEditRoles(Collections.singleton(editRole), vo);
            service.update(vo.getXid(), vo, true, systemSuperadmin);
            VO fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            service.delete(vo.getId(), systemSuperadmin);
            
            //Ensure the mappings are gone
            assertEquals(0, roleService.getDao().getRoles(vo, PermissionService.READ).size());
            assertEquals(0, roleService.getDao().getRoles(vo, PermissionService.EDIT).size());
            
            service.get(vo.getId(), true, systemSuperadmin);
        });
    }
    
    void addRoleToCreatePermission(RoleVO vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            roleService.addRoleToPermission(vo, getCreatePermissionType(), systemSuperadmin);
        }
    }
}
