/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.db.tables.Roles;
import com.infiniteautomation.mango.db.tables.records.RolesRecord;
import com.infiniteautomation.mango.rules.ExpectValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public class RoleServiceTest extends AbstractVOServiceTest<RoleVO, RolesRecord, Roles, RoleDao, RoleService> {

    @Test
    @ExpectValidationException("xid")
    public void cannotInsertNewUserRole() {
        RoleVO vo = new RoleVO(Common.NEW_ID, PermissionHolder.USER_ROLE_XID, "user default");
        service.insert(vo);
    }

    @Test
    @ExpectValidationException("xid")
    public void cannotInsertSuperadminRole() {
        RoleVO vo = new RoleVO(Common.NEW_ID, PermissionHolder.SUPERADMIN_ROLE_XID, "Superadmin default");
        service.insert(vo);
    }

    @Test
    @ExpectValidationException("xid")
    public void cannotModifyUserRole() {
        RoleVO vo = service.get(PermissionHolder.USER_ROLE_XID);
        RoleVO updated = new RoleVO(Common.NEW_ID, vo.getXid(), vo.getName());
        service.update(vo.getXid(), updated);
    }

    @Test
    @ExpectValidationException("xid")
    public void cannotModifySuperadminRole() {
        RoleVO vo = service.get(PermissionHolder.SUPERADMIN_ROLE_XID);
        RoleVO updated = new RoleVO(Common.NEW_ID, vo.getXid(), "Superadmin default changed");
        service.update(vo.getXid(), updated);
    }

    @Override
    @Test
    public void testCount() {
        List<RoleVO> all = service.dao.getAll();
        for (RoleVO vo : all) {
            if (!StringUtils.equals(PermissionHolder.SUPERADMIN_ROLE_XID, vo.getXid())
                    && !StringUtils.equals(PermissionHolder.USER_ROLE_XID, vo.getXid())
                    && !StringUtils.equals(PermissionHolder.ANONYMOUS_ROLE_XID, vo.getXid())) {
                service.delete(vo.getId());
            }
        }

        for (int i = 0; i < 5; i++) {
            insertNewVO(readUser);
        }
        assertEquals(8, service.dao.count());
    }

    @Override
    @Test
    public void testGetAll() {
        List<RoleVO> all = service.dao.getAll();
        for (RoleVO vo : all) {
            if (!StringUtils.equals(PermissionHolder.SUPERADMIN_ROLE_XID, vo.getXid())
                    && !StringUtils.equals(PermissionHolder.USER_ROLE_XID, vo.getXid())
                    && !StringUtils.equals(PermissionHolder.ANONYMOUS_ROLE_XID, vo.getXid())) {
                service.delete(vo.getId());
            }
        }
        List<RoleVO> vos = new ArrayList<>();
        vos.add(service.get(PermissionHolder.SUPERADMIN_ROLE_XID));
        vos.add(service.get(PermissionHolder.USER_ROLE_XID));
        vos.add(service.get(PermissionHolder.ANONYMOUS_ROLE_XID));

        for (int i = 0; i < 5; i++) {
            vos.add(insertNewVO(readUser));
        }
        all = service.dao.getAll();
        for (RoleVO vo : all) {
            RoleVO expected = null;
            for (RoleVO e : vos) {
                if (e.getId() == vo.getId()) {
                    expected = e;
                }
            }
            assertNotNull("Didn't find expected VO", expected);
            assertVoEqual(expected, vo);
        }
    }

    @Test
    public void testQuery() {
        List<RoleVO> vos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vos.add(insertNewVO(readUser));
        }
        AtomicInteger count = new AtomicInteger();
        service.buildQuery().equal("xid", vos.get(0).getXid()).query(r -> {
            assertVoEqual(vos.get(0), r);
            assertEquals(count.incrementAndGet(), 1);
        });
    }

    @Test
    public void adminCanSeeAllRoles() {
        RoleVO vo = insertNewVO(readUser);
        Set<String> roleXids = service.list().stream()
                .map(AbstractVO::getXid)
                .collect(Collectors.toSet());

        Assert.assertTrue("Should see anonymous role", roleXids.contains(PermissionHolder.ANONYMOUS_ROLE_XID));
        Assert.assertTrue("Should see user role", roleXids.contains(PermissionHolder.USER_ROLE_XID));
        Assert.assertTrue("Should see superadmin role", roleXids.contains(PermissionHolder.SUPERADMIN_ROLE_XID));
        Assert.assertTrue("Superadmin should see all roles", roleXids.contains(vo.getXid()));
    }

    @Test
    public void userCanOnlySeeOwnRoles() {
        RoleVO inheritedRole = insertNewVO(readUser);
        RoleVO directlyAssignedRole = newVO(readUser);
        directlyAssignedRole.setInherited(Collections.singleton(inheritedRole.getRole()));
        service.insert(directlyAssignedRole);
        RoleVO roleUserDoesNotHave = insertNewVO(readUser);

        User testUser = createUser("test-user@example.com", "test-user@example.com",
                "test-user@example.com", "test-user@example.com", directlyAssignedRole.getRole());

        runAs.runAs(testUser, () -> {
            Set<String> roleXids = service.list().stream()
                    .map(AbstractVO::getXid)
                    .collect(Collectors.toSet());

            Assert.assertTrue("Should see anonymous role", roleXids.contains(PermissionHolder.ANONYMOUS_ROLE_XID));
            Assert.assertTrue("Should see user role", roleXids.contains(PermissionHolder.USER_ROLE_XID));
            Assert.assertTrue("Should see directly assigned role", roleXids.contains(directlyAssignedRole.getXid()));
            Assert.assertTrue("Should see inherited role", roleXids.contains(inheritedRole.getXid()));
            Assert.assertFalse("Should not see role that user does not have", roleXids.contains(roleUserDoesNotHave.getXid()));
        });
    }

    @Test
    public void canGetRoleUserHas() {
        RoleVO roleUserHas = insertNewVO(readUser);

        User testUser = createUser("test-user@example.com", "test-user@example.com",
                "test-user@example.com", "test-user@example.com", roleUserHas.getRole());

        runAs.runAs(testUser, () -> {
            RoleVO role = service.get(roleUserHas.getXid());
            assertEquals(role.getXid(), roleUserHas.getXid());
            assertEquals(role.getName(), roleUserHas.getName());
            assertEquals(role.getId(), roleUserHas.getId());
        });
    }

    @Test(expected = PermissionException.class)
    public void cantGetRoleUserDoesNotHave() {
        RoleVO roleUserHas = insertNewVO(readUser);
        RoleVO roleUserDoesNotHave = insertNewVO(readUser);

        User testUser = createUser("test-user@example.com", "test-user@example.com",
                "test-user@example.com", "test-user@example.com", roleUserHas.getRole());

        runAs.runAs(testUser, () -> {
            service.get(roleUserDoesNotHave.getXid());
        });
    }

    @Override
    RoleService getService() {
        return Common.getBean(RoleService.class);
    }

    @Override
    RoleDao getDao() {
        return Common.getBean(RoleDao.class);
    }

    @Override
    void assertVoEqual(RoleVO expected, RoleVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
    }

    @Override
    RoleVO newVO(User owner) {
        return new RoleVO(Common.NEW_ID, dao.generateUniqueXid(), "default test role");
    }

    @Override
    RoleVO updateVO(RoleVO existing) {
        return new RoleVO(Common.NEW_ID, existing.getXid(), "updated name");
    }
}
