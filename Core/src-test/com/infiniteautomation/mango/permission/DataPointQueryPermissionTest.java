/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
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
    public void testEmptyPermission() {
        //Insert some data points

        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
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
            public Set<Role> getRoles() {
                return readRoles;
            }

        }, () -> {
            List<Integer> ids = unreadable.stream().map(dp -> dp.getId()).collect(Collectors.toList());

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(0, vos.size());
        });
    }

    @Test
    public void testOrPermission() {
        //Insert some data points

        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.requireAnyRole(readRoles), new MangoPermission());
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
            public Set<Role> getRoles() {
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
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.requireAllRoles(readRoles), new MangoPermission());
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
            public Set<Role> getRoles() {
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
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.requireAllRoles(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());

        RoleDao roleDao = Common.getBean(RoleDao.class);
        for(Role role : readRoles) {
            roleDao.delete(role.getId());
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
            public Set<Role> getRoles() {
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
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.requireAllRoles(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());

        Role role = readRoles.iterator().next();
        Common.getBean(RoleDao.class).delete(role.getId());
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
            public Set<Role> getRoles() {
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
        List<IDataPoint> points = this.createMockDataPoints(5, false, MangoPermission.requireAnyRole(readRoles), new MangoPermission());
        List<IDataPoint> unreadable = this.createMockDataPoints(5, false, new MangoPermission(), new MangoPermission());

        Role role = readRoles.iterator().next();
        Common.getBean(RoleDao.class).delete(role.getId());
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
            public Set<Role> getRoles() {
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
}
