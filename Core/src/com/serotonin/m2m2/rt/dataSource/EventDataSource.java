/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataSource;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

abstract public class EventDataSource extends DataSourceRT {
    protected List<DataPointRT> dataPoints = new ArrayList<DataPointRT>();

    public EventDataSource(DataSourceVO<?> vo) {
        super(vo);
    }

    @Override
    public void addDataPoint(DataPointRT dataPoint) {
        synchronized (pointListChangeLock) {
            // Remove any existing instances of the points.
            dataPoints.remove(dataPoint);
            dataPoints.add(dataPoint);
        }
    }

    @Override
    public void removeDataPoint(DataPointRT dataPoint) {
        synchronized (pointListChangeLock) {
            dataPoints.remove(dataPoint);
        }
    }

    @Override
    public void setPointValue(DataPointRT dataPoint, PointValueTime valueTime, SetPointSource source) {
        // Typically, event based data sources cannot set point values, so don't make subclasses implement this.
    }
}
