/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataSource;

import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

/**
 *
 * @author Terry Packer
 */
public class MockPollingDataSource extends PollingDataSource<MockDataSourceVO>{

    /**
     */
    public MockPollingDataSource(MockDataSourceVO vo) {
        super(vo);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.dataSource.PollingDataSource#doPoll(long)
     */
    @Override
    protected void doPoll(long time) { }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.dataSource.DataSourceRT#setPointValueImpl(com.serotonin.m2m2.rt.dataImage.DataPointRT, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public void setPointValueImpl(DataPointRT dataPoint, PointValueTime valueTime,
            SetPointSource source) {
        dataPoint.setPointValue(valueTime, source);
    }

}
