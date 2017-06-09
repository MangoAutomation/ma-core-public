/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PublicFileStoreWritePermissionDefinition;

/**
 * Access to the default public store
 * 
 * @author Jared Wiltshire
 */
public class PublicFileStoreDefinition extends FileStoreDefinition{

	public static final String NAME = "public";
	
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
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.FileStoreDefinition#getWritePermissionTypeName()
	 */
	@Override
	protected String getWritePermissionTypeName() {
		return PublicFileStoreWritePermissionDefinition.TYPE_NAME;
	}

}
