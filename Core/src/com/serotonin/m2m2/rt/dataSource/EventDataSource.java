/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataSource;

import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

abstract public class EventDataSource<T extends DataSourceVO> extends DataSourceRT<T> {

    public EventDataSource(T vo) {
        super(vo);
    }

    @Override
    public void setPointValueImpl(DataPointRT dataPoint, PointValueTime valueTime, SetPointSource source) {
        // Typically, event based data sources cannot set point values, so don't make subclasses implement this.
        throw new UnsupportedOperationException("Not implemented for " + getClass());
    }
}
