/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 * @author Terry Packer
 *
 */
public abstract class PointEventDetectorDefinition<T extends AbstractPointEventDetectorVO<T>> extends EventDetectorDefinition<T>{

	public static final String SOURCE_ID_COLUMN_NAME = "dataPointId";
	
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
		return EventType.EventTypeNames.DATA_POINT;
	}
	
}
