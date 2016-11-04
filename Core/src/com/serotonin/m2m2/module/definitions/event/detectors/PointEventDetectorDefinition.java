/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.module.EventDetectorDefinition;

/**
 * @author Terry Packer
 *
 */
public abstract class PointEventDetectorDefinition extends EventDetectorDefinition{

	public static final String SOURCE_ID_COLUMN_NAME = "dataPointId";
	public static final String SOURCE_TYPE_NAME = "DATA_POINT";
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getSourceIdColumnName()
	 */
	@Override
	public String getSourceIdColumnName() {
		return SOURCE_ID_COLUMN_NAME;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getSourceTypeName()
	 */
	@Override
	public String getSourceTypeName() {
		return SOURCE_TYPE_NAME;
	}
	
}
