/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class DataPointServiceTest<T extends DataSourceVO> extends AbstractVOServiceWithPermissionsTest<DataPointVO, DataPointTableDefinition, DataPointDao, DataPointService> {

    private DataSourceService dataSourceService;

    public DataPointServiceTest() {
    }

    @Override
    public void before() {
        super.before();
        dataSourceService = Common.getBean(DataSourceService.class);
    }

    @Test
    @Override
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            getService().permissionService.runAsSystemAdmin(() -> {
                DataSourceVO ds = dataSourceService.get(vo.getDataSourceId());
                ds.setEditRoles(Collections.singleton(editRole));
                dataSourceService.update(ds.getXid(), ds);
                setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
                vo.setSetRoles(Collections.singleton(roleService.getUserRole()));
            });
            getService().permissionService.runAs(editUser, () -> {
                service.insert(vo);
            });
        });
    }

    @Test
    @Override
    public void testUserCanDelete() {
        runTest(() -> {
            getService().permissionService.runAs(editUser, () -> {
                DataPointVO vo = newVO(readUser);
                addRoleToCreatePermission(editRole);
                setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
                setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
                vo.setSetRoles(Collections.singleton(roleService.getUserRole()));
                vo = service.insert(vo);
                service.delete(vo.getId());
            });
        });
    }

    @Test
    @Override
    public void testUserEditRole() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            vo.setSetRoles(Collections.singleton(PermissionHolder.USER_ROLE.get()));
            getService().permissionService.runAsSystemAdmin(() -> {
                service.insert(vo);
            });
            getService().permissionService.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                service.update(fromDb.getXid(), fromDb);
                DataPointVO updated = service.get(fromDb.getId());
                assertVoEqual(fromDb, updated);
            });
        });
    }

    @Test(expected = PermissionException.class)
    @Override
    public void testUserEditRoleFails() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            setReadRoles(Collections.singleton(PermissionHolder.USER_ROLE.get()), vo);
            setEditRoles(Collections.emptySet(), vo);
            vo.setSetRoles(Collections.singleton(PermissionHolder.USER_ROLE.get()));
            getService().permissionService.runAsSystemAdmin(() -> {
                service.insert(vo);
            });
            getService().permissionService.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                service.update(fromDb.getXid(), fromDb);
                DataPointVO updated = service.get(fromDb.getId());
                assertVoEqual(fromDb, updated);
            });
        });
    }

    @Test()
    @Override
    public void testCannotRemoveEditAccess() {
        //No-op will be tested in the data source service test
    }

    @Test()
    @Override
    public void testAddEditRoleUserDoesNotHave() {
        //No-op will be tested in the data source service test
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                DataPointVO vo = insertNewVO(editUser);
                setReadRoles(Collections.singleton(readRole), vo);
                setEditRoles(Collections.singleton(editRole), vo);
                vo.setSetRoles(Collections.singleton(readRole));
                service.update(vo.getXid(), vo);
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());

                //Ensure the mappings are gone
                assertEquals(0, roleService.getDao().getRoles(vo, PermissionService.READ).size());
                assertEquals(0, roleService.getDao().getRoles(vo, PermissionService.SET).size());

                service.get(vo.getId());
            });
        });
    }

    @Test(expected = ValidationException.class)
    public void testCannotRemoveSetAccess() {
        DataPointVO vo = newVO(editUser);
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        vo.setSetRoles(Collections.singleton(roleService.getUserRole()));
        getService().permissionService.runAsSystemAdmin(() -> {
            service.insert(vo);
        });
        getService().permissionService.runAs(readUser, () -> {
            DataPointVO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            fromDb.setSetRoles(Collections.emptySet());
            service.update(fromDb.getXid(), fromDb);
        });
    }

    @Test(expected = ValidationException.class)
    public void testAddSetRoleUserDoesNotHave() {
        DataPointVO vo = newVO(editUser);
        setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
        setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
        vo.setSetRoles(Collections.singleton(roleService.getUserRole()));
        getService().permissionService.runAsSystemAdmin(() -> {
            service.insert(vo);
        });
        getService().permissionService.runAs(readUser, () -> {
            DataPointVO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            fromDb.setSetRoles(Collections.singleton(roleService.getSuperadminRole()));
            service.update(fromDb.getXid(), fromDb);
        });
    }

    @Override
    String getCreatePermissionType() {
        return null;
    }

    @Override
    void setReadRoles(Set<Role> roles, DataPointVO vo) {
        vo.setReadRoles(roles);
    }

    @Override
    void setEditRoles(Set<Role> roles, DataPointVO vo) {
        getService().permissionService.runAsSystemAdmin(() -> {
            DataSourceVO ds = dataSourceService.get(vo.getDataSourceId());
            ds.setEditRoles(roles);
            dataSourceService.update(ds.getXid(), ds);
        });
    }

    @Override
    DataPointService getService() {
        return Common.getBean(DataPointService.class);
    }

    @Override
    DataPointDao getDao() {
        return DataPointDao.getInstance();
    }

    @Override
    void assertVoEqual(DataPointVO expected, DataPointVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());

        assertEquals(expected.getDataSourceId(), actual.getDataSourceId());
        assertEquals(expected.getPointLocator().getDataTypeId(), actual.getPointLocator().getDataTypeId());

        assertRoles(expected.getReadRoles(), actual.getReadRoles());
        assertRoles(expected.getSetRoles(), actual.getSetRoles());
    }

    @Override
    DataPointVO newVO(User user) {
        return getService().permissionService.runAsSystemAdmin(() -> {;
        DataSourceVO mock = dataSourceService.insert(createDataSource());
        //Create the point
        DataPointVO vo = new DataPointVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        vo.setDataSourceId(mock.getId());
        vo.setPointLocator(new MockPointLocatorVO());
        //TODO Flesh out all fields

        return vo;
        });
    }

    @Override
    DataPointVO updateVO(DataPointVO existing) {
        DataPointVO copy = existing.copy();

        return copy;
    }

    @SuppressWarnings("unchecked")
    T createDataSource() {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        dsVo.setName("permissions_test_datasource");
        return (T) dsVo;
    }

    @Override
    void addReadRoleToFail(Role role, DataPointVO vo) {
        vo.getReadRoles().add(role);
    }

    @Override
    void addEditRoleToFail(Role role, DataPointVO vo) {
        vo.getSetRoles().add(role);
    }
}
