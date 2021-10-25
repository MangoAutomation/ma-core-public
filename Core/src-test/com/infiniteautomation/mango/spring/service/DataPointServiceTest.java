/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.records.DataPointsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.rules.ExpectValidationException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.usage.DataPointUsageStatistics;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class DataPointServiceTest<T extends DataSourceVO> extends AbstractVOServiceWithPermissionsTest<DataPointVO, DataPointsRecord, DataPoints, DataPointDao, DataPointService> {

    private DataSourceService dataSourceService;
    private PointValueDaoSQL pointValueDaoSQL;


    public DataPointServiceTest() {
    }

    @Override
    public void before() {
        super.before();
        dataSourceService = Common.getBean(DataSourceService.class);
        pointValueDaoSQL = Common.getBean(PointValueDaoSQL.class);
    }

    @Test
    @Override
    public void testCreatePrivilegeSuccess() {
        DataPointVO vo = newVO(editUser);
        addRoleToCreatePermission(editRole, vo);
        setReadPermission(MangoPermission.requireAnyRole(editRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        vo.setSetPermission(MangoPermission.requireAnyRole(editRole));
        runAs.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    @Override
    public void testUserCanDelete() {
        DataPointVO vo = newVO(readUser);
        addRoleToCreatePermission(editRole, vo);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
        service.insert(vo);
        runAs.runAs(editUser, () -> {
            service.delete(vo.getId());
        });
    }

    @Test
    @Override
    public void testUserEditRole() {
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
    }

    @Test(expected = PermissionException.class)
    @Override
    public void testUserEditRoleFails() {
        DataPointVO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE), vo);
        setEditPermission(MangoPermission.superadminOnly(), vo);
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
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        DataPointVO vo = insertNewVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(readRole), vo);
        setEditPermission(MangoPermission.requireAnyRole(editRole), vo);
        vo.setSetPermission(MangoPermission.requireAnyRole(readRole));
        service.update(vo.getXid(), vo);
        DataPointVO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        service.delete(vo.getId());

        service.get(vo.getId());
    }

    @Test
    public void cannotRemoveUserRoleFromSetPermission() {
        validation.expectValidationException("setPermission");

        DataPointVO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            DataPointVO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            fromDb.setName("read user edited me");
            fromDb.setSetPermission(MangoPermission.superadminOnly());
            service.update(fromDb.getXid(), fromDb);
        });
    }

    @Test
    @ExpectValidationException("setPermission")
    public void testSetRolesCannotBeNull() {
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
    }

    @Override
    @Test
    public void testEditRolesCannotBeNull() {
        //Not a thing
    }

    @Test
    @Override
    public void testAddReadRoleUserDoesNotHave() {
        validation.expectValidationException(getReadPermissionContextKey());
        DataPointVO vo = newVO(readUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            DataPointVO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            setReadPermission(MangoPermission.superadminOnly(), fromDb);
            service.update(fromDb.getId(), fromDb);
        });
    }

    /**
     * There will be 2 validation messages about this, must retain permission AND cannot add/remove a role you do not have
     */
    @Override
    @Test
    public void testAddEditRoleUserDoesNotHave() {
        validation.expectValidationException(getEditPermissionContextKey());
        DataPointVO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        vo.setSetPermission(MangoPermission.requireAnyRole(roleService.getUserRole()));
        setEditPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        service.insert(vo);
        runAs.runAs(readUser, () -> {
            DataPointVO fromDb = service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            setEditPermission(MangoPermission.superadminOnly(), fromDb);
            service.update(fromDb.getId(), fromDb);
        });
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

        assertEquals(expected.getSeriesId(), actual.getSeriesId());

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

    @Test
    public void testIsEnabled() {
        DataPointVO point = newVO(readUser);
        DataPointVO inserted = service.insert(point);
        boolean enabled = dao.isEnabled(inserted.getId());
        assertFalse(enabled);

        inserted.setEnabled(true);
        DataPointVO updated = service.update(inserted.getId(), inserted);
        enabled = dao.isEnabled(updated.getId());
        assertTrue(enabled);
    }

    @Test
    public void testCountPointsForDataSourceType() {
        createMockDataPoints(5);
        int pointCount = dao.countPointsForDataSourceType(MockDataSourceDefinition.TYPE_NAME);
        assertEquals(5, pointCount);
    }

    @Test
    public void testTopPointHistoryCountsSql() {
        DataPointVO point = newVO(readUser);
        DataPointVO inserted = service.insert(point);
        for (int i = 0; i < 5; i++) {
            PointValueTime newPvt = new PointValueTime(i, System.currentTimeMillis());
            pointValueDaoSQL.savePointValueSync(inserted, newPvt, null);
        }
        List<PointHistoryCount> historyCounts = dao.getTopPointHistoryCounts();
        assertEquals(1, historyCounts.size());

        DataPointVO fromDB = service.get(inserted.getId());
        PointHistoryCount history = historyCounts.get(0);
        assertEquals(fromDB.getId(), history.getPointId());
        assertEquals(fromDB.getExtendedName(), history.getPointName());
        assertEquals(5, history.getCount());
    }

    @Test
    public void testUsage() {
        for (int i = 0; i < 5; i++) {
            DataPointVO point = newVO(readUser);
            service.insert(point);
        }
        List<DataPointUsageStatistics> stats = dao.getUsage();
        assertEquals(1, stats.size());

        DataPointUsageStatistics stat = stats.get(0);
        assertEquals((Integer) 5, stat.getCount());
        assertEquals(MockDataSourceDefinition.TYPE_NAME, stat.getDataSourceType());
    }

    @SuppressWarnings("unchecked")
    T createDataSource() {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        dsVo.setName("permissions_test_datasource");
        return (T) dsVo;
    }

    @Override
    String getReadPermissionContextKey() {
        return "readPermission";
    }

    @Override
    String getEditPermissionContextKey() {
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
