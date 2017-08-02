/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.permissions;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.module.PermissionDefinition;

/**
 * 
 * @author Terry Packer
 */
public class DocsFileStoreReadPermissionDefinition extends PermissionDefinition{

		public static final String TYPE_NAME = "filestore.docs.read";
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionKey()
		 */
		@Override
		public String getPermissionKey() {
			return "filestore.docs.permission.read";
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.module.PermissionDefinition#getPermissionTypeName()
		 */
		@Override
		public String getPermissionTypeName() {
			return TYPE_NAME;
		}

		/* (non-Javadoc)
		* @see com.serotonin.m2m2.module.PermissionDefinition#getDefaultGroups()
		*/
		@Override
		public List<String> getDefaultGroups() {
	        List<String> groups = new ArrayList<String>();
	        groups.add("user");
	        return groups;
		}
}
