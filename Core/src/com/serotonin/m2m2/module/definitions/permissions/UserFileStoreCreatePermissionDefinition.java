/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * 
 * @author Terry Packer
 */
public class UserFileStoreCreatePermissionDefinition extends PermissionDefinition{

		public static final String TYPE_NAME = "filestore.user.create";
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionKey()
		 */
		@Override
		public String getPermissionKey() {
			return "filestore.user.permission.create";
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionTypeName()
		 */
		@Override
		public String getPermissionTypeName() {
			return TYPE_NAME;
		}

}
