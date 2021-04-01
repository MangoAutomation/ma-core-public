/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Table;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicVOServiceWithPermissionsTestBase<VO extends AbstractBasicVO, R extends Record, TABLE extends Table<R>, DAO extends AbstractBasicDao<VO, R, TABLE>, SERVICE extends AbstractBasicVOService<VO, DAO>> extends AbstractBasicVOServiceTest<VO, R, TABLE, DAO, SERVICE> {

    public AbstractBasicVOServiceWithPermissionsTestBase() {

    }

    /**
     * The type name for the create permission of the VO
     * @return
     */
    abstract String getCreatePermissionType();
    /**
     * replace the read roles with these
     * @param permission
     * @param vo
     */
    abstract void setReadPermission(MangoPermission permission, VO vo);
    /**
     * Get the roles and attempt to add this to the roles, should fail
     *
     * @param role
     * @param vo
     */
    abstract void addReadRoleToFail(Role role, VO vo);

    /**
     * Get the context key used in the validation of the read roles
     * @return
     */
    abstract String getReadRolesContextKey();

    /**
     * Replace the edit roles with these
     * @param permission
     * @param vo
     */
    abstract void setEditPermission(MangoPermission permission, VO vo);
    /**
     * Get the roles and attempt to add this to the roles, should fail
     *
     * @param role
     * @param vo
     */
    abstract void addEditRoleToFail(Role role, VO vo);

    /**
     * Get the context key used in the validation of the edit roles
     * @return
     */
    abstract String getEditRolesContextKey();

    @Test(expected = PermissionException.class)
    public void testCreatePrivilegeFails() {
        VO vo = newVO(editUser);
        runAs.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        VO vo = newVO(editUser);
        addRoleToCreatePermission(editRole);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        runAs.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    public void testUserReadRole() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        service.insert(vo);

        runAs.runAs(readUser, () -> {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
        });
    }

    @Test(expected = PermissionException.class)
    public void testUserReadRoleFails() {
        runTest(() -> {
            VO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(Collections.emptySet()), vo);
            service.insert(vo);

            runAs.runAs(readUser, () -> {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
            });
        }, getReadRolesContextKey());
    }

    @Test
    public void testReadRolesCannotBeNull() {
        runTest(() -> {
            VO vo = newVO(readUser);
            setReadPermission(null, vo);
            service.insert(vo);
        }, getReadRolesContextKey());
    }

    @Test
    public void testCannotRemoveReadAccess() {
        runTest(() -> {
            VO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                setReadPermission(MangoPermission.requireAnyRole(Collections.emptySet()), fromDb);
                service.update(fromDb.getId(), fromDb);
            });
        }, getReadRolesContextKey());
    }

    /**
     * There will be 2 validation messages about this, must retain permission AND cannot add/remove a role you do not have
     */
    @Test
    public void testAddReadRoleUserDoesNotHave() {
        runTest(() -> {
            VO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), fromDb);
                service.update(fromDb.getId(), fromDb);
            });
        }, getReadRolesContextKey());
    }

    @Test
    public void testUserEditRole() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            service.update(fromDb.getId(), fromDb);
            VO updated = service.get(fromDb.getId());
            assertVoEqual(fromDb, updated);
        });
    }

    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(Collections.emptySet()), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            service.update(fromDb.getId(), fromDb);
            VO updated = service.get(fromDb.getId());
            assertVoEqual(fromDb, updated);
        });
    }

    public void testEditRolesCannotBeNull() {
        runTest(() -> {
            VO vo = newVO(editUser);
            setEditPermission(null, vo);
            service.insert(vo);
        }, getEditRolesContextKey());
    }

    @Test
    public void testCannotRemoveEditAccess() {
        runTest(() -> {
            VO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                setEditPermission(MangoPermission.requireAnyRole(Collections.emptySet()), fromDb);
                service.update(fromDb.getId(), fromDb);
            });
        }, getEditRolesContextKey());
    }

    /**
     * There will be 2 validation messages about this, must retain permission AND cannot add/remove a role you do not have
     */
    @Test
    public void testAddEditRoleUserDoesNotHave() {
        runTest(() -> {
            VO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                setEditPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), fromDb);
                service.update(fromDb.getId(), fromDb);
            });
        }, getEditRolesContextKey());
    }

    @Test
    public void testUserCanDelete() {
        VO vo = newVO(readUser);
        addRoleToCreatePermission(editRole);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        runAs.runAs(editUser, () -> {
            VO newVO = service.insert(vo);
            service.delete(newVO.getId());
        });
    }

    @Test(expected = PermissionException.class)
    public void testUserDeleteFails() {
        VO vo = newVO(readUser);
        service.insert(vo);
        runAs.runAs(editUser, () -> {
            service.delete(vo.getId());
        });
    }

    @Test(expected = PermissionException.class)
    public void testSuperadminReadRole() {
        VO vo = newVO(readUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), vo);
        VO saved = service.insert(vo);
        runAs.runAs(readUser, () -> {
            service.get(saved.getId());
        });
    }

    @Test(expected = PermissionException.class)
    public void testSuperadminEditRole() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), vo);
        VO saved = service.insert(vo);
        runAs.runAs(readUser, () -> {
            VO fromDb = service.get(saved.getId());
            assertVoEqual(saved, fromDb);
            service.update(fromDb.getId(), fromDb);
        });
    }

    @Test
    public void testDeleteRoleUpdateVO() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(readRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        service.insert(vo);
        VO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        roleService.delete(editRole.getId());
        roleService.delete(readRole.getId());
        VO updated = service.get(fromDb.getId());
        setReadPermission(MangoPermission.requireAnyRole(Collections.emptySet()), fromDb);
        setEditPermission(MangoPermission.requireAnyRole(Collections.emptySet()), fromDb);
        assertVoEqual(fromDb, updated);
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        VO vo = insertNewVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(readRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        service.update(vo.getId(), vo);
        VO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        service.delete(vo.getId());

        service.get(vo.getId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotModifyReadRoles() {
        VO vo = newVO(readUser);
        setReadPermission(MangoPermission.requireAnyRole(readRole), vo);
        VO saved = service.insert(vo);
        addReadRoleToFail(editRole, saved);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotModifySetRoles() {
        VO vo = newVO(readUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), vo);
        VO saved = service.insert(vo);
        addEditRoleToFail(editRole, saved);
    }

    @Test
    public void testCountQueryReadPermissionEnforcement() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole(), editRole), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            ConditionSortLimit conditions = new ConditionSortLimit(null, null, null, 0);
            int count = getService().customizedCount(conditions);
            assertEquals(0, count);
        });
        VO vo2 = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole(), editRole), vo2);
        service.insert(vo2);
        runAs.runAs(editUser, () -> {
            ConditionSortLimit conditions = new ConditionSortLimit(null, null, null, 0);
            int count = getService().customizedCount(conditions);
            assertEquals(2, count);
        });
    }

    @Test
    public void testCountQueryEditPermissionEnforcement() {
        VO vo = newVO(readUser);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole(), editRole), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            ConditionSortLimit conditions = new ConditionSortLimit(null, null, null, 0);
            int count = getService().customizedCount(conditions);
            assertEquals(0, count);
        });
    }

    @Test
    public void testQueryReadPermissionEnforcement() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole(), editRole), vo);
        VO saved = service.insert(vo);
        runAs.runAs(readUser, () -> {
            ConditionSortLimit conditions = new ConditionSortLimit(null, null, 1, 0);
            AtomicInteger count = new AtomicInteger();
            getService().customizedQuery(conditions, (item) -> {
                count.getAndIncrement();
            });
            assertEquals(0, count.get());
        });
        runAs.runAs(editUser, () -> {
            Condition c = getDao().getIdField().eq(saved.getId());
            ConditionSortLimit conditions = new ConditionSortLimit(c, null, null, null);
            AtomicInteger count = new AtomicInteger();
            getService().customizedQuery(conditions, (item) -> {
                count.getAndIncrement();
                assertEquals(saved.getId(), item.getId());
            });
            assertEquals(1, count.get());
        });
    }

    @Test
    public void testQueryEditPermissionEnforcement() {
        VO vo = newVO(readUser);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole(), editRole), vo);
        VO saved = service.insert(vo);
        runAs.runAs(readUser, () -> {
            ConditionSortLimit conditions = new ConditionSortLimit(null, null, 1, 0);
            AtomicInteger count = new AtomicInteger();
            getService().customizedQuery(conditions, (item) -> {
                count.getAndIncrement();
            });
            assertEquals(0, count.get());
        });
    }

    void addRoleToCreatePermission(Role vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            PermissionDefinition def = ModuleRegistry.getPermissionDefinition(getCreatePermissionType());
            Set<Set<Role>> roleSet = def.getPermission().getRoles();
            Set<Set<Role>> newRoles = new HashSet<>();
            newRoles.add(Collections.singleton(vo));
            for (Set<Role> roles : roleSet) {
                newRoles.add(new HashSet<>(roles));
            }
            Common.getBean(SystemPermissionService.class).update(new MangoPermission(newRoles), def);
        }
    }
}
