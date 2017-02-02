/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataSource;

import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;

/**
 * Mock Point Locator RT, useful in testing.
 * 
 * @author Terry Packer
 *
 */
public class MockPointLocatorRT extends PointLocatorRT<MockPointLocatorVO>{

	public MockPointLocatorRT(MockPointLocatorVO vo){
		super(vo);
	}
}
