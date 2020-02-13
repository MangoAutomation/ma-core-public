/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public class RoleServiceTest extends AbstractVOServiceTest<RoleVO, RoleTableDefinition, RoleDao, RoleService> {

    /**
     * Test the mapping table
     */
    @Test
    public void mappingTableWorks() {
        runTest(() -> {
            //First add a role to the edit permission of a point
            //Create data source?
            MockDataSourceVO ds = new MockDataSourceVO();
            ds.setXid("DS_TEST1");
            ds.setName("TEST");
            DataSourceDao.getInstance().insert(ds);

            DataPointVO dp = new DataPointVO();
            dp.setXid("DP_PERM_TEST");
            dp.setName("test name");
            dp.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));
            dp.setDataSourceId(ds.getId());
            DataPointDao.getInstance().insert(dp);

            //TODO Wire into data point service?
            //Mock up the insert into the mapping table for now
            RoleService roleService = Common.getBean(RoleService.class);
            roleService.addRoleToVoPermission(readRole, dp, PermissionService.READ, PermissionHolder.SYSTEM_SUPERADMIN);
            roleService.addRoleToVoPermission(editRole, dp, PermissionService.EDIT, PermissionHolder.SYSTEM_SUPERADMIN);

            PermissionService service = Common.getBean(PermissionService.class);

            assertTrue(service.hasPermission(readUser, dp, PermissionService.READ));
            assertTrue(service.hasPermission(editUser, dp, PermissionService.EDIT));

            assertFalse(service.hasPermission(readUser, dp, PermissionService.SET));
            assertFalse(service.hasPermission(setUser, dp, PermissionService.SET));
        });
    }

    @Test(expected = ValidationException.class)
    public void cannotInsertNewUserRole() {
        RoleVO vo = new RoleVO(Common.NEW_ID, PermissionHolder.USER_ROLE_XID, "user default");
        getService().permissionService.runAsSystemAdmin(() -> {
            service.insert(vo);
        });
    }

    @Test(expected = ValidationException.class)
    public void cannotInsertSuperadminRole() {
        RoleVO vo = new RoleVO(Common.NEW_ID, PermissionHolder.SUPERADMIN_ROLE_XID, "Superadmin default");
        getService().permissionService.runAsSystemAdmin(() -> {
            service.insert(vo);
        });
    }

    @Test(expected = ValidationException.class)
    public void cannotModifyUserRole() {
        getService().permissionService.runAsSystemAdmin(() -> {
            RoleVO vo = service.get(PermissionHolder.USER_ROLE_XID);
            RoleVO updated = new RoleVO(Common.NEW_ID, vo.getXid(), vo.getName());
            service.update(vo.getXid(), updated);
        });
    }

    @Test(expected = ValidationException.class)
    public void cannotModifySuperadminRole() {
        getService().permissionService.runAsSystemAdmin(() -> {
            RoleVO vo = service.get(PermissionHolder.SUPERADMIN_ROLE_XID);
            RoleVO updated = new RoleVO(Common.NEW_ID, vo.getXid(), "Superadmin default changed");
            service.update(vo.getXid(), updated);
        });
    }

    @Override
    @Test
    public void testCount() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                List<RoleVO> all = service.dao.getAll();
                for(RoleVO vo : all) {
                    if(!StringUtils.equals(PermissionHolder.SUPERADMIN_ROLE_XID,vo.getXid())
                            && !StringUtils.equals(PermissionHolder.USER_ROLE_XID,vo.getXid())) {
                        service.delete(vo.getId());
                    }
                }

                List<RoleVO> vos = new ArrayList<>();
                for(int i=0; i<5; i++) {
                    vos.add(insertNewVO(readUser));
                }
                assertEquals(7, service.dao.count());
            });
        });
    }

    @Override
    @Test
    public void testGetAll() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                List<RoleVO> all = service.dao.getAll();
                for(RoleVO vo : all) {
                    if(!StringUtils.equals(PermissionHolder.SUPERADMIN_ROLE_XID,vo.getXid())
                            && !StringUtils.equals(PermissionHolder.USER_ROLE_XID,vo.getXid())) {
                        service.delete(vo.getId());
                    }
                }
                List<RoleVO> vos = new ArrayList<>();
                vos.add(service.get(PermissionHolder.SUPERADMIN_ROLE_XID));
                vos.add(service.get(PermissionHolder.USER_ROLE_XID));

                for(int i=0; i<5; i++) {
                    vos.add(insertNewVO(readUser));
                }
                all = service.dao.getAll();
                for(RoleVO vo : all) {
                    RoleVO expected = null;
                    for(RoleVO e : vos) {
                        if(e.getId() == vo.getId()) {
                            expected = e;
                        }
                    }
                    assertNotNull("Didn't find expected VO", expected);
                    assertVoEqual(expected, vo);
                }
            });
        });
    }

    @Override
    RoleService getService() {
        return Common.getBean(RoleService.class);
    }

    @Override
    RoleDao getDao() {
        return RoleDao.getInstance();
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
