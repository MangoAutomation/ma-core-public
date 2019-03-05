/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.Permission;

/**
 * @author Terry Packer
 *
 */
public class SuperadminPermissionDefinition extends PermissionDefinition {
	public static final String GROUP_NAME = "superadmin";
	public static final String PERMISSION = "permissions.superadmin";
	
	private final Set<String> roles;
	
    public SuperadminPermissionDefinition() {
        this.roles = new HashSet<>();
        this.roles.add(GROUP_NAME);
    }
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionKey()
	 */
	@Override
	public String getPermissionKey() {
		return "systemSettings.permissions.superadmin";
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
		groups.add(GROUP_NAME);
		return groups;
	}
	
	@Override
	public Permission getPermission() {
	    return new Permission(PERMISSION, roles);
	}
}
