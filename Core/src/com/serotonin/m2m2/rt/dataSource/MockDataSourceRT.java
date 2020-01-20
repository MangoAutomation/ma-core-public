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
public class MockDataSourceRT extends DataSourceRT<MockDataSourceVO>{

    public static final int POLL_ABORTED_EVENT = 1;

    public MockDataSourceRT(MockDataSourceVO vo) {
        super(vo);
    }

    @Override
    public void setPointValueImpl(DataPointRT dataPoint, PointValueTime newValue,
            SetPointSource source) {
        dataPoint.setPointValue(newValue, source);
    }

}
