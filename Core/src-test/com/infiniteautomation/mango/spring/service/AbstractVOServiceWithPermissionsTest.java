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
import com.serotonin.m2m2.db.dao.AbstractVoDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractVOServiceWithPermissionsTest<VO extends AbstractVO, R extends Record, TABLE extends Table<R>, DAO extends AbstractVoDao<VO,R,TABLE>, SERVICE extends AbstractVOService<VO, DAO>> extends AbstractVOServiceTest<VO, R, TABLE, DAO, SERVICE> {

    public AbstractVOServiceWithPermissionsTest() {

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
     * Get the context key used in the validation of the read roles
     * @return
     */
    abstract String getReadPermissionContextKey();

    /**
     * Replace the edit roles with these
     * @param permission
     * @param vo
     */
    abstract void setEditPermission(MangoPermission permission, VO vo);

    /**
     * Get the context key used in the validation of the edit roles
     * @return
     */
    abstract String getEditPermissionContextKey();

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
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(Collections.emptySet()), vo);
        service.insert(vo);

        runAs.runAs(readUser, () -> {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
        });
    }

    @Test
    public void testReadRolesCannotBeNull() {
        runTest(() -> {
            VO vo = newVO(readUser);
            setReadPermission(null, vo);
            service.insert(vo);
        }, getReadPermissionContextKey());
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
        }, getReadPermissionContextKey());
    }

    /**
     * There will be 2 validation messages about this, must retain permission AND cannot add/remove a role you do not have
     */
    @Test
    public void testAddReadRoleUserDoesNotHave() {
        runTest(() -> {
            VO vo = newVO(readUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                VO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), fromDb);
                service.update(fromDb.getId(), fromDb);
            });

        }, getReadPermissionContextKey());
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

    @Test
    public void testEditRolesCannotBeNull() {
        runTest(() -> {
            VO vo = newVO(editUser);
            setEditPermission(null, vo);
            service.insert(vo);
        }, getEditPermissionContextKey());
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
        }, getEditPermissionContextKey());
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
        }, getEditPermissionContextKey());
    }

    @Test
    public void testUserCanDelete() {
        VO vo = newVO(readUser);
        addRoleToCreatePermission(editRole);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        VO newVO = service.insert(vo);
        runAs.runAs(readUser, () -> {
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
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            service.get(vo.getId());
        });
    }

    @Test(expected = PermissionException.class)
    public void testSuperadminEditRole() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            VO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
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
        service.update(fromDb.getId(), fromDb);
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

    public int queryForName(String name) {
        Condition c = getDao().getNameField().eq(name);
        ConditionSortLimit conditions = new ConditionSortLimit(c, null, null, 0);
        AtomicInteger count = new AtomicInteger();
        getService().customizedQuery(conditions, (item) -> {
            count.getAndIncrement();
            assertEquals(name, item.getName());
        });
        return count.get();
    }

    public int countForName(String name) {
        Condition c = getDao().getNameField().eq(name);
        ConditionSortLimit conditions = new ConditionSortLimit(c, null, null, 0);
        return getService().customizedCount(conditions);
    }

    @Test
    public void testCountQueryReadPermissionEnforcement() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(readRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        service.insert(vo);

        runAs.runAs(readUser, () -> assertEquals(1, countForName(vo.getName())));
        runAs.runAs(editUser, () -> assertEquals(0, countForName(vo.getName())));
    }

    @Test
    public void testCountQueryEditPermissionEnforcement() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(editRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        service.insert(vo);

        runAs.runAs(readUser, () -> assertEquals(0, countForName(vo.getName())));
        runAs.runAs(editUser, () -> assertEquals(1, countForName(vo.getName())));
    }

    @Test
    public void testQueryReadPermissionEnforcement() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(readRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        service.insert(vo);

        runAs.runAs(readUser, () -> assertEquals(1, queryForName(vo.getName())));
        runAs.runAs(editUser, () -> assertEquals(0, queryForName(vo.getName())));
    }

    @Test
    public void testQueryEditPermissionEnforcement() {
        VO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(editRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        VO saved = service.insert(vo);

        runAs.runAs(readUser, () -> assertEquals(0, queryForName(vo.getName())));
        runAs.runAs(editUser, () -> assertEquals(1, queryForName(vo.getName())));
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

    void removeRoleFromCreatePermission(Role vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            PermissionDefinition def = ModuleRegistry.getPermissionDefinition(getCreatePermissionType());
            MangoPermission permission = def.getPermission();
            Common.getBean(SystemPermissionService.class).update(new MangoPermission(permission.withoutRole(vo).getRoles()), def);
        }
    }
}
