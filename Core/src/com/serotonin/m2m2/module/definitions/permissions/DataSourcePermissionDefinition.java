/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * Global can create data sources permission (also can edit any)
 * 
 * @author Terry Packer
 *
 */
public class DataSourcePermissionDefinition extends PermissionDefinition {
	
    public static String PERMISSION = "permissionDatasource";
    
    @Override
    public TranslatableMessage getDescription() {
		return new TranslatableMessage("systemSettings.permissions.datasourceManagement");
	}

	@Override
	public String getPermissionTypeName() {
		return PERMISSION;
	}
	
}
