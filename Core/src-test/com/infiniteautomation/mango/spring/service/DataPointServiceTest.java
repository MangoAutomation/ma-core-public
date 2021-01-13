/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.ShouldNeverHappenException;
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
            addRoleToCreatePermission(editRole, vo);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
            runAs.runAs(editUser, () -> {
                service.insert(vo);
            });
        });
    }

    @Test
    @Override
    public void testUserCanDelete() {
        runTest(() -> {
            DataPointVO vo = newVO(readUser);
            addRoleToCreatePermission(editRole, vo);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
            service.insert(vo);
            runAs.runAs(editUser, () -> {
                service.delete(vo.getId());
            });
        });
    }

    @Test
    @Override
    public void testUserEditRole() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
            service.insert(vo);
            runAs.runAs(readUser, () -> {
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
            setReadPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE), vo);
            setEditPermission(MangoPermission.requireAnyRole(Collections.emptySet()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                service.update(fromDb.getXid(), fromDb);
                DataPointVO updated = service.get(fromDb.getId());
                assertVoEqual(fromDb, updated);
            });
        });
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            DataPointVO vo = insertNewVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(readRole), vo);
            setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(readRole));
            service.update(vo.getXid(), vo);
            DataPointVO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            service.delete(vo.getId());

            service.get(vo.getId());
        });
    }

    @Test
    public void testCannotRemoveSetAccess() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                fromDb.setSetPermission(new MangoPermission());
                service.update(fromDb.getXid(), fromDb);
            });
        }, "setPermission");
    }

    @Test
    public void testSetRolesCannotBeNull() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(null);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                fromDb.setSetPermission(new MangoPermission());
                service.update(fromDb.getXid(), fromDb);
            });
        }, "setPermission");
    }

    @Override
    @Test
    public void testEditRolesCannotBeNull() {
        //Not a thing
    }

    @Test
    public void testAddSetRoleUserDoesNotHave() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                fromDb.setName("read user edited me");
                fromDb.setSetPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()));
                service.update(fromDb.getXid(), fromDb);
            });
        }, "setPermission");
    }

    @Test
    @Override
    public void testAddReadRoleUserDoesNotHave() {
        runTest(() -> {
            DataPointVO vo = newVO(readUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), fromDb);
                service.update(fromDb.getId(), fromDb);
            });
        }, getReadRolesContextKey());
    }

    /**
     * There will be 2 validation messages about this, must retain permission AND cannot add/remove a role you do not have
     */
    @Override
    @Test
    public void testAddEditRoleUserDoesNotHave() {
        runTest(() -> {
            DataPointVO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
            setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            service.insert(vo);
            runAs.runAs(readUser, () -> {
                DataPointVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                setEditPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole()), fromDb);
                service.update(fromDb.getId(), fromDb);
            });
        }, getEditRolesContextKey());
    }

    @Override
    String getCreatePermissionType() {
        return null;
    }

    @Override
    void setReadPermission(MangoPermission permission, DataPointVO vo) {
        vo.setReadPermission(permission);
    }

    @Override
    void setEditPermission(MangoPermission permission, DataPointVO vo) {
        vo.setEditPermission(permission);
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

        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getEditPermission(), actual.getEditPermission());
        assertPermission(expected.getSetPermission(), actual.getSetPermission());
    }

    @Override
    DataPointVO newVO(User user) {
        DataSourceVO mock = dataSourceService.insert(createDataSource());
        //Create the point
        DataPointVO vo = new DataPointVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        vo.setDataSourceId(mock.getId());
        vo.setPointLocator(new MockPointLocatorVO());
        //TODO Flesh out all fields

        return vo;
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
        vo.getReadPermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getReadRolesContextKey() {
        return "readPermission";
    }

    @Override
    void addEditRoleToFail(Role role, DataPointVO vo) {
        vo.getEditPermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getEditRolesContextKey() {
        return "editPermission";
    }

    @Override
    void addRoleToCreatePermission(Role vo) {
        throw new ShouldNeverHappenException("use addRoleToCreatePermission(Role,DataPointVO)");
    }

    void addRoleToCreatePermission(Role role, DataPointVO vo) {
        DataSourceVO ds = dataSourceService.get(vo.getDataSourceId());
        Set<Set<Role>> roles = new HashSet<>(ds.getEditPermission().getRoles());
        Set<Role> createRole = new HashSet<>();
        createRole.add(role);
        roles.add(createRole);
        ds.setEditPermission(new MangoPermission(roles));
        dataSourceService.update(ds.getXid(), ds);
    }
}
