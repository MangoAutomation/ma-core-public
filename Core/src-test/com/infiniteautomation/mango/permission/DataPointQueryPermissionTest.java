/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.QueryBuilder;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class DataPointQueryPermissionTest extends MangoTestBase {

    @Test
    public void testOrPermission() {
        //Insert some data points

        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.createOrSet(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());
        DataPointService service = Common.getBean(DataPointService.class);
        service.getPermissionService().runAs(new PermissionHolder() {

            @Override
            public String getPermissionHolderName() {
                return "Test";
            }

            @Override
            public boolean isPermissionHolderDisabled() {
                return false;
            }

            @Override
            public Set<Role> getAllInheritedRoles() {
                return readRoles;
            }

        }, () -> {
            List<Integer> ids = points.stream().map(dp -> dp.getId()).collect(Collectors.toList());
            ids.addAll(unreadable.stream().map(dp -> dp.getId()).collect(Collectors.toList()));

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(points.size(), vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }
        });
    }

    @Test
    public void testAndPermission() {
        //Insert some data points
        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.createAndSet(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());
        DataPointService service = Common.getBean(DataPointService.class);
        service.getPermissionService().runAs(new PermissionHolder() {

            @Override
            public String getPermissionHolderName() {
                return "Test";
            }

            @Override
            public boolean isPermissionHolderDisabled() {
                return false;
            }

            @Override
            public Set<Role> getAllInheritedRoles() {
                return readRoles;
            }

        }, () -> {
            List<Integer> ids = points.stream().map(dp -> dp.getId()).collect(Collectors.toList());
            ids.addAll(unreadable.stream().map(dp -> dp.getId()).collect(Collectors.toList()));

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(points.size(), vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }
        });
    }

    @Test
    public void testDeleteAllRoles() {
        //Insert some data points
        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.createAndSet(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());

        for(Role role : readRoles) {
            RoleDao.getInstance().delete(role.getId());
        }
        DataPointService service = Common.getBean(DataPointService.class);
        service.getPermissionService().runAs(new PermissionHolder() {

            @Override
            public String getPermissionHolderName() {
                return "Test";
            }

            @Override
            public boolean isPermissionHolderDisabled() {
                return false;
            }

            @Override
            public Set<Role> getAllInheritedRoles() {
                //This is odd as these roles should not be on a user either, but wanted to t
                return readRoles;
            }

        }, () -> {
            List<Integer> ids = points.stream().map(dp -> dp.getId()).collect(Collectors.toList());
            ids.addAll(unreadable.stream().map(dp -> dp.getId()).collect(Collectors.toList()));

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(0, vos.size());
        });
    }

    /**
     * Delete a single role from an and set (this results in the user still having permission)
     */
    @Test
    public void testDeleteSingleRoleFromAndSet() {
        //Insert some data points
        Set<Role> readRoles = this.createRoles(3).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.createAndSet(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());

        Role role = readRoles.iterator().next();
        RoleDao.getInstance().delete(role.getId());
        readRoles.remove(role);

        DataPointService service = Common.getBean(DataPointService.class);
        service.getPermissionService().runAs(new PermissionHolder() {

            @Override
            public String getPermissionHolderName() {
                return "Test";
            }

            @Override
            public boolean isPermissionHolderDisabled() {
                return false;
            }

            @Override
            public Set<Role> getAllInheritedRoles() {
                return readRoles;
            }

        }, () -> {
            List<Integer> ids = points.stream().map(dp -> dp.getId()).collect(Collectors.toList());
            ids.addAll(unreadable.stream().map(dp -> dp.getId()).collect(Collectors.toList()));

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(points.size(), vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }

        });
    }

    @Test
    public void testDeleteSingleRoleFromOrSet() {
        //Insert some data points
        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.createOrSet(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());

        Role role = readRoles.iterator().next();
        RoleDao.getInstance().delete(role.getId());
        readRoles.remove(role);

        DataPointService service = Common.getBean(DataPointService.class);
        service.getPermissionService().runAs(new PermissionHolder() {

            @Override
            public String getPermissionHolderName() {
                return "Test";
            }

            @Override
            public boolean isPermissionHolderDisabled() {
                return false;
            }

            @Override
            public Set<Role> getAllInheritedRoles() {
                return readRoles;
            }

        }, () -> {
            List<Integer> ids = points.stream().map(dp -> dp.getId()).collect(Collectors.toList());
            ids.addAll(unreadable.stream().map(dp -> dp.getId()).collect(Collectors.toList()));

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(points.size(), vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }
        });
    }

    /**
     * Test to ensure an un-used permission is deleted
     */
    @Test
    public void testDeleteDataPoint() {
        //Insert some data points
        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(1, false, MangoPermission.createOrSet(readRoles), new MangoPermission());

        DataPointService service = Common.getBean(DataPointService.class);
        service.getPermissionService().runAs(new PermissionHolder() {

            @Override
            public String getPermissionHolderName() {
                return "Test";
            }

            @Override
            public boolean isPermissionHolderDisabled() {
                return false;
            }

            @Override
            public Set<Role> getAllInheritedRoles() {
                return readRoles;
            }

        }, () -> {
            List<Integer> ids = points.stream().map(dp -> dp.getId()).collect(Collectors.toList());

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(points.size(), vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }

            //Delete a point
            DataPointDao.getInstance().delete(vos.get(0));

            ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
            ejt.setDataSource(Common.databaseProxy.getDataSource());
            List<Integer> permissions = ejt.query("SELECT id from permissions", new RowMapper<Integer>() {

                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }

            });
            assertEquals(0, permissions.size());
        });
    }
}
