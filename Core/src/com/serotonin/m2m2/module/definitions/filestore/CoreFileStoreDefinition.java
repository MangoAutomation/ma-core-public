/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreWritePermissionDefinition;

/**
 * Access to the default core store
 * 
 * @author Terry Packer
 */
public class CoreFileStoreDefinition extends FileStoreDefinition{

	public static final String NAME = "default";
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.FileStoreDefinition#getStoreName()
	 */
	@Override
	public String getStoreName() {
		return NAME;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.FileStoreDefinition#getReadPermissionTypeName()
	 */
	@Override
	protected String getReadPermissionTypeName() {
		return CoreFileStoreReadPermissionDefinition.TYPE_NAME;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.FileStoreDefinition#getWritePermissionTypeName()
	 */
	@Override
	protected String getWritePermissionTypeName() {
		return CoreFileStoreWritePermissionDefinition.TYPE_NAME;
	}

}
