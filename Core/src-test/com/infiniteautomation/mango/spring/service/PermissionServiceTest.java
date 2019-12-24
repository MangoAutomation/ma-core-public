/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class PermissionServiceTest extends MangoTestBase {
    
    protected RoleService roleService;
    
    protected PermissionHolder systemSuperadmin;
    
    protected User readUser;
    protected User editUser;
    protected User setUser;
    protected User allUser;
    
    protected RoleVO readRole;
    protected RoleVO editRole;
    protected RoleVO setRole;
    protected RoleVO deleteRole;
    
    public PermissionServiceTest() {
        super(false, 9001);
    }

    @Before
    public void setupRoles() {
        roleService = Common.getBean(RoleService.class);
        
        systemSuperadmin = PermissionHolder.SYSTEM_SUPERADMIN;

        //Add some roles
        readRole = new RoleVO();
        readRole.setXid("read-role");
        readRole.setName("Role to allow reading.");
        roleService.insert(readRole, systemSuperadmin);
        
        editRole = new RoleVO();
        editRole.setXid("edit-role");
        editRole.setName("Role to allow editing.");
        roleService.insert(editRole, systemSuperadmin);
        
        setRole = new RoleVO();
        setRole.setXid("set-role");
        setRole.setName("Role to allow setting.");
        roleService.insert(setRole, systemSuperadmin);
        
        deleteRole = new RoleVO();
        deleteRole.setXid("delete-role");
        deleteRole.setName("Role to allow deleting.");
        roleService.insert(deleteRole, systemSuperadmin);
        
        readUser = createUser("readUser", "readUser", "password", "readUser@example.com", readRole);
        editUser = createUser("editUser", "editUser", "password", "editUser@example.com", editRole);
        setUser = createUser("setUser", "setUser", "password", "setUser@example.com", setRole);
        allUser = createUser("allUser", "allUser", "password", "allUser@example.com", readRole, editRole, setRole, deleteRole);
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
}
