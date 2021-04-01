/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.infiniteautomation.mango.db.tables.Roles;
import com.infiniteautomation.mango.db.tables.records.RolesRecord;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public class RoleServiceTest extends AbstractVOServiceTest<RoleVO, RolesRecord, Roles, RoleDao, RoleService> {

    @Test
    public void cannotInsertNewUserRole() {
        runTest(() -> {
            RoleVO vo = new RoleVO(Common.NEW_ID, PermissionHolder.USER_ROLE_XID, "user default");
            service.insert(vo);
        }, "xid", "xid");
    }

    @Test
    public void cannotInsertSuperadminRole() {
        runTest(() -> {
            RoleVO vo = new RoleVO(Common.NEW_ID, PermissionHolder.SUPERADMIN_ROLE_XID, "Superadmin default");
            service.insert(vo);
        }, "xid", "xid");
    }

    @Test
    public void cannotModifyUserRole() {
        runTest(() -> {
            RoleVO vo = service.get(PermissionHolder.USER_ROLE_XID);
            RoleVO updated = new RoleVO(Common.NEW_ID, vo.getXid(), vo.getName());
            service.update(vo.getXid(), updated);
        }, "xid");
    }

    @Test
    public void cannotModifySuperadminRole() {
        runTest(() -> {
            RoleVO vo = service.get(PermissionHolder.SUPERADMIN_ROLE_XID);
            RoleVO updated = new RoleVO(Common.NEW_ID, vo.getXid(), "Superadmin default changed");
            service.update(vo.getXid(), updated);
        }, "xid");
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

        List<RoleVO> vos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vos.add(insertNewVO(readUser));
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
        RoleVO vo = new RoleVO(Common.NEW_ID, dao.generateUniqueXid(), "default test role");
        return vo;
    }

    @Override
    RoleVO updateVO(RoleVO existing) {
        return new RoleVO(Common.NEW_ID, existing.getXid(), "updated name");
    }
}
