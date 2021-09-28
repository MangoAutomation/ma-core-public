/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.QueryBuilder;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.infiniteautomation.mango.spring.components.RunAs;

/**
 *
 * @author Terry Packer
 */
public class DataPointPermissionTest extends MangoTestBase {

    private RunAs runAs;

    @Before
    public void init() {
        this.runAs = Common.getBean(RunAs.class);
    }

    /**
     * Update permission ensure no orphaned minterms or permissions exist
     */
    @Test
    public void testUpdatePermission() {
        DataPointDao dao = Common.getBean(DataPointDao.class);

        //Insert some data points
        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        DataPointVO point = (DataPointVO)this.createMockDataPoints(1, false, MangoPermission.requireAnyRole(readRoles), new MangoPermission()).get(0);

        //Save for later to see if the permission is removed
        int permissionId = point.getReadPermission().getId();

        //Update permission
        point.setReadPermission(MangoPermission.requireAnyRole(readRoles.iterator().next()));
        dao.update(point.getId(), point);

        //Check for the recently orphaned permission (it should not be there)
        ExtendedJdbcTemplate ejt = Common.getBean(DatabaseProxy.class).getJdbcTemplate();
        List<Integer> permissionIds = ejt.query("SELECT id from permissions WHERE id=" + permissionId, new RowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }

        });
        assertEquals(0, permissionIds.size());

        //Check for orphaned minterm mappings
        List<Integer> mintermIds = ejt.query("SELECT mintermId from permissionsMinterms WHERE permissionId=" + permissionId, new RowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }

        });
        assertEquals(0, mintermIds.size());
    }

    /**
     * Test to ensure an un-used permission is deleted
     */
    @Test
    public void testDeleteDataPoint() {
        //Insert some data points
        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(1, false, MangoPermission.requireAnyRole(readRoles), new MangoPermission());

        DataPointService service = Common.getBean(DataPointService.class);
        runAs.runAs(new PermissionHolder() {

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

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(points.size(), vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }

            ExtendedJdbcTemplate ejt = Common.getBean(DatabaseProxy.class).getJdbcTemplate();
            List<Integer> existing = ejt.query("SELECT id from permissions", new RowMapper<Integer>() {

                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }

            });

            //Delete the source and point
            DataSourceDao.getInstance().delete(vos.get(0).getDataSourceId());


            List<Integer> permissions = ejt.query("SELECT id from permissions", new RowMapper<Integer>() {

                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }

            });

            //We should have removed 1 permission
            assertEquals(existing.size() - 1, permissions.size());
        });
    }

    @Test
    public void testDeleteDataPoints() {
        //Insert some data points
        Set<Role> readRoles = this.createRoles(2).stream().map(r -> r.getRole()).collect(Collectors.toSet());
        List<IDataPoint> points = this.createMockDataPoints(2, false, MangoPermission.requireAnyRole(readRoles), new MangoPermission());

        DataPointService service = Common.getBean(DataPointService.class);
        runAs.runAs(new PermissionHolder() {

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

            QueryBuilder<DataPointVO> query = service.buildQuery().in("id", ids.toArray());
            List<DataPointVO> vos = query.query();
            assertEquals(points.size(), vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }

            ExtendedJdbcTemplate ejt = Common.getBean(ExtendedJdbcTemplate.class);
            List<Integer> existing = ejt.query("SELECT id from permissions", new RowMapper<Integer>() {

                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }

            });

            //Delete a point
            DataPointDao.getInstance().delete(vos.get(0));

            List<Integer> permissions = ejt.query("SELECT id from permissions", new RowMapper<Integer>() {

                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }

            });

            //The set and read permission for point 2 still exist
            assertEquals(existing.size(), permissions.size());

            //ensure all minterms ect still exist for the un-deleted point
            vos = query.query();
            assertEquals(1, vos.size());
            for(DataPointVO vo : vos) {
                assertTrue(points.contains(vo));
            }
        });
    }

}
