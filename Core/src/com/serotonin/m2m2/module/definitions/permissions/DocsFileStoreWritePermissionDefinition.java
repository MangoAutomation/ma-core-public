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
public class DocsFileStoreWritePermissionDefinition extends PermissionDefinition{

		public static final String TYPE_NAME = "filestore.docs.write";
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionKey()
		 */
		@Override
		public String getPermissionKey() {
			return "filestore.docs.permission.write";
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionTypeName()
		 */
		@Override
		public String getPermissionTypeName() {
			return TYPE_NAME;
		}

}
