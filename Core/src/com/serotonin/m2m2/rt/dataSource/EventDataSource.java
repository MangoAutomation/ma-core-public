/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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

    /**
     * Do not call while holding read lock
     */
    @Override
    public void addDataPoint(DataPointRT dataPoint) {
        pointListChangeLock.writeLock().lock();
        try {
            // Replace data point
            dataPoints.put(dataPoint.getId(), dataPoint);
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    /**
     * Do not call while holding read lock
     */
    @Override
    public void removeDataPoint(DataPointRT dataPoint) {
        pointListChangeLock.writeLock().lock();
        try {
            dataPoints.remove(dataPoint.getId(), dataPoint);
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    @Override
    public void setPointValueImpl(DataPointRT dataPoint, PointValueTime valueTime, SetPointSource source) {
        // Typically, event based data sources cannot set point values, so don't make subclasses implement this.
    }
}
