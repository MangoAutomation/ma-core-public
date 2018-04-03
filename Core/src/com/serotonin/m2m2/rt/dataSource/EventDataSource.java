/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataSource;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

abstract public class EventDataSource<T extends DataSourceVO<?>> extends DataSourceRT<T> {
    protected List<DataPointRT> dataPoints = new ArrayList<>();

    public EventDataSource(T vo) {
        super(vo);
    }

    @Override
    public void addDataPoint(DataPointRT dataPoint) {
        pointListChangeLock.writeLock().lock();
        try {
            // Remove any existing instances of the points.
            dataPoints.remove(dataPoint);
            dataPoints.add(dataPoint);
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    @Override
    public void removeDataPoint(DataPointRT dataPoint) {
        pointListChangeLock.writeLock().lock();
        try {
            dataPoints.remove(dataPoint);
        } finally {
            pointListChangeLock.writeLock().unlock();
        }
    }

    @Override
    public void setPointValueImpl(DataPointRT dataPoint, PointValueTime valueTime, SetPointSource source) {
        // Typically, event based data sources cannot set point values, so don't make subclasses implement this.
    }
}
