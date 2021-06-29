/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
