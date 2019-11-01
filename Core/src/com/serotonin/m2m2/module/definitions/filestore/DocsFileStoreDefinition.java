/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.filestore;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreWritePermissionDefinition;

/**
 * 
 * @author Terry Packer
 */
public class DocsFileStoreDefinition extends FileStoreDefinition{

	public static final String NAME = "docs";
	
	@Override
	public TranslatableMessage getStoreDescription() {
	    return new TranslatableMessage("filestore.docs.description");
	}
	
	@Override
	public String getStoreName() {
		return NAME;
	}

	@Override
	protected String getReadPermissionTypeName() {
		return DocsFileStoreReadPermissionDefinition.TYPE_NAME;
	}

	@Override
	protected String getWritePermissionTypeName() {
		return DocsFileStoreWritePermissionDefinition.TYPE_NAME;
	}
	
}
