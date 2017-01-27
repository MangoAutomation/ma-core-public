/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataSource;

import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;


/**
 * Mock Data Source Runtime, useful when testing
 * 
 * @author Terry Packer
 *
 */
public class MockDataSourceRT extends DataSourceRT{

	/**
	 * @param vo
	 */
	public MockDataSourceRT(MockDataSourceVO vo) {
		super(vo);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataSource.DataSourceRT#setPointValue(com.serotonin.m2m2.rt.dataImage.DataPointRT, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public void setPointValueImpl(DataPointRT dataPoint, PointValueTime newValue,
			SetPointSource source) {
		dataPoint.setPointValue(newValue, source);
	}	
	
}
