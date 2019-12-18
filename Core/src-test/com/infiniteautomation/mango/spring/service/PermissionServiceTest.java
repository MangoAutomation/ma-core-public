/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;

/**
 * @author Terry Packer
 *
 */
public class PermissionServiceTest extends MangoTestBase {
    
    private static boolean setup = false;
    
    private User readUser;
    private User editUser;
    private User setUser;
    private User allUser;
    
    private RoleVO readRole;
    private RoleVO editRole;
    private RoleVO setRole;
    
    
    public PermissionServiceTest() {
        super(true, 9001);
    }

    @Before
    public void setupPermissions() {
        
        if(setup) {
            return;
        }
        
        //Add a user with no roles
        readUser = createUser("readUser", "readUser", "password", "readUser@example.com", "read-role");
        editUser = createUser("editUser", "editUser", "password", "editUser@example.com", "edit-role");
        setUser = createUser("setUser", "setUser", "password", "setUser@example.com", "set-role");
        allUser = createUser("allUser", "allUser", "password", "allUser@example.com", "read-role,edit-role,set-role");
        
        //Add some roles
        RoleService roleService = Common.getBean(RoleService.class);
        readRole = new RoleVO();
        readRole.setXid("read-role");
        readRole.setName("Role to allow reading.");
        roleService.insert(readRole, PermissionHolder.SYSTEM_SUPERADMIN);
        
        editRole = new RoleVO();
        editRole.setXid("edit-role");
        editRole.setName("Role to allow reading.");
        roleService.insert(editRole, PermissionHolder.SYSTEM_SUPERADMIN);
        
        setRole = new RoleVO();
        setRole.setXid("set-role");
        setRole.setName("Role to allow reading.");
        roleService.insert(setRole, PermissionHolder.SYSTEM_SUPERADMIN);
        
        setup = true;
    }
    
    @Test
    public void testHasAnyRole() {
        PermissionService service = Common.getBean(PermissionService.class);
        
        assertTrue(service.hasAnyRole(readUser, Arrays.asList(readRole)));
    
        assertFalse(service.hasAnyRole(readUser, Arrays.asList(editRole)));
        
        assertTrue(service.hasAnyRole(readUser, Arrays.asList(readRole, editRole)));

        assertFalse(service.hasAnyRole(readUser, Arrays.asList(editRole, setRole)));

        assertTrue(service.hasAnyRole(allUser, Arrays.asList(readRole, editRole, setRole)));

        assertTrue(service.hasAnyRole(readUser, Arrays.asList(readRole, editRole)));

    }
    
    /**
     * Test the mapping table
     */
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
        
        PermissionService service = Common.getBean(PermissionService.class);

        assertTrue(service.hasPermission(readUser, dp, PermissionService.READ));

        assertFalse(service.hasPermission(readUser, dp, PermissionService.SET));

    }
    
    //TODO Test delete mapping on VO deletion
}
