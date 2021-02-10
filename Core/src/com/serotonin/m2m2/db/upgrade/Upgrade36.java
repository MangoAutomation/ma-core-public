/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import com.infiniteautomation.mango.db.tables.RoleInheritance;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Makes the superadmin role inherit the user role, and the user role inherit the anonymous role
 *
 * @author Jared Wiltshire
 */
public class Upgrade36 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        RoleInheritance ri = RoleInheritance.ROLE_INHERITANCE;

        create.insertInto(ri, ri.roleId, ri.inheritedRoleId)
                .values(PermissionHolder.SUPERADMIN_ROLE.getId(), PermissionHolder.USER_ROLE.getId())
                .values(PermissionHolder.USER_ROLE.getId(), PermissionHolder.ANONYMOUS_ROLE.getId())
                .execute();
    }

    @Override
    protected String getNewSchemaVersion() {
        return "37";
    }
}
