/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreWritePermissionDefinition;

/**
 * 
 * @author Terry Packer
 */
public class DocsFileStoreDefinition extends FileStoreDefinition{

	public static final String NAME = "docs";
	
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
		return DocsFileStoreReadPermissionDefinition.TYPE_NAME;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.FileStoreDefinition#getWritePermissionTypeName()
	 */
	@Override
	protected String getWritePermissionTypeName() {
		return DocsFileStoreWritePermissionDefinition.TYPE_NAME;
	}
	
}
