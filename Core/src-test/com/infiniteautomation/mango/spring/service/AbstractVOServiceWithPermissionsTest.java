/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import com.infiniteautomation.mango.spring.db.AbstractTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractVOServiceWithPermissionsTest<VO extends AbstractVO<VO>, TABLE extends AbstractTableDefinition, DAO extends AbstractDao<VO,TABLE>, SERVICE extends AbstractVOService<VO,TABLE,DAO>> extends AbstractVOServiceTest<VO, TABLE, DAO, SERVICE> {

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
    abstract void setReadRoles(Set<Role> roles, VO vo);
    abstract void setEditRoles(Set<Role> roles, VO vo);

    @Test(expected = PermissionException.class)
    public void testCreatePrivilegeFails() {
        VO vo = newVO();
        Common.setUser(editUser);
        try {
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            VO vo = newVO();
            addRoleToCreatePermission(editRole);
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            Common.setUser(editUser);
            try {
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test
    public void testUserReadRole() {
        runTest(() -> {
            VO vo = newVO();
            Common.setUser(systemSuperadmin);
            try {
                setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(readUser);
            try {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = PermissionException.class)
    public void testUserReadRoleFails() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.emptySet(), vo);
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(readUser);
            try {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = ValidationException.class)
    public void testReadRolesCannotBeNull() {
        VO vo = newVO();
        setReadRoles(null, vo);
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
    }

    @Test(expected = ValidationException.class)
    public void testCannotRemoveReadAccess() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
        Common.setUser(readUser);
        try {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            setReadRoles(Collections.emptySet(), fromDb);
            service.update(fromDb.getXid(), fromDb);
        }finally {
            Common.removeUser();
        }
    }

    @Test(expected = ValidationException.class)
    public void testAddReadRoleUserDoesNotHave() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
        Common.setUser(readUser);
        try {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            setReadRoles(Collections.singleton(roleService.getSuperadminRole()), fromDb);
            service.update(fromDb.getXid(), fromDb);
        }finally {
            Common.removeUser();
        }
    }

    @Test
    public void testUserEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(readUser);
            try {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                service.update(fromDb.getXid(), fromDb);
                VO updated = service.get(fromDb.getId());
                assertVoEqual(fromDb, updated);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.emptySet(), vo);
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(readUser);
            try {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                service.update(fromDb.getXid(), fromDb);
                VO updated = service.get(fromDb.getId());
                assertVoEqual(fromDb, updated);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = ValidationException.class)
    public void testEditRolesCannotBeNull() {
        VO vo = newVO();
        setEditRoles(null, vo);
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
    }

    @Test(expected = ValidationException.class)
    public void testCannotRemoveEditAccess() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
        Common.setUser(readUser);
        try {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            setEditRoles(Collections.emptySet(), fromDb);
            service.update(fromDb.getXid(), fromDb);
        }finally {
            Common.removeUser();
        }
    }

    @Test(expected = ValidationException.class)
    public void testAddEditRoleUserDoesNotHave() {
        VO vo = newVO();
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
        Common.setUser(readUser);
        try {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            setEditRoles(Collections.singleton(roleService.getSuperadminRole()), fromDb);
            service.update(fromDb.getXid(), fromDb);
        }finally {
            Common.removeUser();
        }
    }

    @Test
    public void testUserCanDelete() {
        runTest(() -> {
            VO vo = newVO();
            addRoleToCreatePermission(editRole);
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            Common.setUser(editUser);
            try {
                vo = service.insert(vo);
                service.delete(vo.getId());
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = PermissionException.class)
    public void testUserDeleteFails() {
        runTest(() -> {
            VO vo = newVO();
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(editUser);
            try {
                service.delete(vo.getId());
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = PermissionException.class)
    public void testSuperadminReadRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getSuperadminRole()), vo);
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(editUser);
            try {
                service.get(vo.getId());
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = PermissionException.class)
    public void testSuperadminEditRole() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getSuperadminRole()), vo);
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(readUser);
            try {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                service.update(fromDb.getXid(), fromDb);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test
    public void testDeleteRoleUpdateVO() {
        runTest(() -> {
            VO vo = newVO();
            setReadRoles(Collections.singleton(readRole), vo);
            setEditRoles(Collections.singleton(editRole), vo);
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                roleService.delete(editRole.getId());
                roleService.delete(readRole.getId());
                VO updated = service.get(fromDb.getId());
                setReadRoles(Collections.emptySet(), fromDb);
                setEditRoles(Collections.emptySet(), fromDb);
                assertVoEqual(fromDb, updated);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            Common.setUser(systemSuperadmin);
            try {
                VO vo = insertNewVO();
                setReadRoles(Collections.singleton(readRole), vo);
                setEditRoles(Collections.singleton(editRole), vo);

                service.update(vo.getXid(), vo);
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());

                //Ensure the mappings are gone
                assertEquals(0, roleService.getDao().getRoles(vo, PermissionService.READ).size());
                assertEquals(0, roleService.getDao().getRoles(vo, PermissionService.EDIT).size());

                service.get(vo.getId());
            }finally {
                Common.removeUser();
            }
        });
    }

    void addRoleToCreatePermission(Role vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            roleService.addRoleToPermission(vo, getCreatePermissionType(), systemSuperadmin);
        }
    }
}
