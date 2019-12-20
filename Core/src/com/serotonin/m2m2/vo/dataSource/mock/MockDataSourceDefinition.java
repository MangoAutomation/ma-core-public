/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.dataSource.mock;

import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

/**
 * @author Terry Packer
 *
 */
public class MockDataSourceDefinition extends DataSourceDefinition{

    public static final String TYPE_NAME = "MOCK";
    
	@Override
	public String getDataSourceTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return ""; //
	}

	@Override
	protected DataSourceVO<?> createDataSourceVO() {
		return new MockDataSourceVO();
	}
}
