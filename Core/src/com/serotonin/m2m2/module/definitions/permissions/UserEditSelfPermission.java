/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * This permission determines who can edit themselves
 * 
 * @author Terry Packer
 *
 */
public class UserEditSelfPermission extends PermissionDefinition{
    public static final String PERMISSION = "permissions.user.editSelf";
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionKey()
     */
    @Override
    public String getPermissionKey() {
        return "systemSettings.permissions.userEditSelf";
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionTypeName()
     */
    @Override
    public String getPermissionTypeName() {
        return PERMISSION;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PermissionDefinition#getDefaultGroups()
     */
    @Override
    public List<String> getDefaultGroups() {
        List<String> groups = new ArrayList<String>();
        groups.add(Permissions.USER_DEFAULT);
        return groups;
    }
}