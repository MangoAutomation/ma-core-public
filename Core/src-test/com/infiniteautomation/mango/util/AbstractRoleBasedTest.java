/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util;

import com.infiniteautomation.mango.spring.service.RoleService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

public class AbstractRoleBasedTest extends MangoTestBase {

    protected User readUser;
    protected User editUser;
    protected User setUser;
    protected User deleteUser;
    protected User allUser;

    protected Role readRole;
    protected Role editRole;
    protected Role deleteRole;
    protected Role setRole;

    protected RoleService roleService;

    protected void setupRoles() {
        roleService = Common.getBean(RoleService.class);

        //Add some roles
        RoleVO temp = new RoleVO(Common.NEW_ID, "read-role", "Role to allow reading.");
        roleService.insert(temp);
        readRole = new Role(temp);

        temp = new RoleVO(Common.NEW_ID, "edit-role", "Role to allow editing.");
        roleService.insert(temp);
        editRole = new Role(temp);

        temp = new RoleVO(Common.NEW_ID, "set-role", "Role to allow setting.");
        roleService.insert(temp);
        setRole = new Role(temp);

        temp = new RoleVO(Common.NEW_ID, "delete-role", "Role to allow deleting.");
        roleService.insert(temp);
        deleteRole = new Role(temp);

        readUser = createUser("readUser", "readUser", "password", "readUser@example.com", readRole);
        editUser = createUser("editUser", "editUser", "password", "editUser@example.com", editRole);
        setUser = createUser("setUser", "setUser", "password", "setUser@example.com", setRole);
        deleteUser = createUser("deleteUser", "deleteUser", "password", "deleteUser@example.com", deleteRole);
        allUser = createUser("allUser", "allUser", "password", "allUser@example.com", readRole, editRole, setRole, deleteRole);
    }

}
