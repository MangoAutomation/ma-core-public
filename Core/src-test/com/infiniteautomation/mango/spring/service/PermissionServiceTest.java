/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class PermissionServiceTest extends ServiceTestBase {
    
    public PermissionServiceTest() {
        super(true, 9001);
    }

    
    @Test
    public void testHasAnyRole() {
        PermissionService service = Common.getBean(PermissionService.class);
        
        assertTrue(service.hasAnyRole(readUser, new HashSet<>(Arrays.asList(readRole))));
    
        assertFalse(service.hasAnyRole(readUser, new HashSet<>(Arrays.asList(editRole))));
        
        assertTrue(service.hasAnyRole(readUser, new HashSet<>(Arrays.asList(readRole, editRole))));

        assertFalse(service.hasAnyRole(readUser, new HashSet<>(Arrays.asList(editRole, setRole))));

        assertTrue(service.hasAnyRole(allUser, new HashSet<>(Arrays.asList(readRole, editRole, setRole))));

        assertTrue(service.hasAnyRole(readUser, new HashSet<>(Arrays.asList(readRole, editRole))));

    }
    
    /**
     * Test the mapping table
     */
    @Test
    public void testHasPermission() {
        //First add a role to the edit permission of a point
        //Create data source?
        MockDataSourceVO ds = new MockDataSourceVO();
        ds.setXid("DS_TEST1");
        ds.setName("TEST");
        DataSourceDao.getInstance().save(ds);
        
        DataPointVO dp = new DataPointVO();
        dp.setXid("DP_PERM_TEST");
        dp.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));
        dp.setDataSourceId(ds.getId());
        dp.setReadPermission("read-role");
        DataPointDao.getInstance().save(dp);
        
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
        
    }
    
    //TODO Test delete mapping on VO deletion
    //TODO Test not being able to modify the admin role
    //TODO Test not being able to modify the user role
}
