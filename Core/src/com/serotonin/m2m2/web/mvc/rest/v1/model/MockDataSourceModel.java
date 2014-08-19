/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.MockDataSource;

/**
 * Useful for testing
 * 
 * @author Terry Packer
 *
 */
public class MockDataSourceModel extends AbstractDataSourceModel<MockDataSource>{

	/**
	 * @param data
	 */
	public MockDataSourceModel(DataSourceVO<MockDataSource> data) {
		super(data);
	}

	public MockDataSourceModel(){
		super(new MockDataSource());
	}
}
