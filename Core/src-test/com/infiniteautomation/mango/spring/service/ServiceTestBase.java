/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.junit.Before;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Base class to test the service layer implementations
 * 
 * @author Terry Packer
 *
 */
public class ServiceTestBase extends MangoTestBase {

    public ServiceTestBase() {
        
    }
    
    public ServiceTestBase(boolean enableWebDb, int webDbPort) {
        super(enableWebDb, webDbPort);
    }
    
    protected PermissionHolder systemSuperadmin;
    
    protected User readUser;
    protected User editUser;
    protected User setUser;
    protected User deleteUser;
    protected User allUser;
    
    protected RoleVO readRole;
    protected RoleVO editRole;
    protected RoleVO deleteRole;
    protected RoleVO setRole;
    
    @Before
    public void setupRoles() {
        
        systemSuperadmin = PermissionHolder.SYSTEM_SUPERADMIN;
        
        //Add a user with no roles
        readUser = createUser("readUser", "readUser", "password", "readUser@example.com", "read-role");
        editUser = createUser("editUser", "editUser", "password", "editUser@example.com", "edit-role");
        setUser = createUser("setUser", "setUser", "password", "setUser@example.com", "set-role");
        deleteUser = createUser("deleteUser", "deleteUser", "password", "deleteUser@example.com", "delete-role");
        allUser = createUser("allUser", "allUser", "password", "allUser@example.com", "read-role,edit-role,set-role,delete-roll");
        
        //Add some roles
        RoleService roleService = Common.getBean(RoleService.class);
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

    }
    
}
