/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * @author Matthew Lohbihler
 */
public class PointValueFacade {
    private final int dataPointId;
    private final DataPointRT point;
    private final PointValueDao pointValueDao;

    public PointValueFacade(int dataPointId) {
        this.dataPointId = dataPointId;
        point = Common.runtimeManager.getDataPoint(dataPointId);
        pointValueDao = Common.databaseProxy.newPointValueDao();
    }

    //
    //
    // Single point value
    //
    public PointValueTime getPointValueBefore(long time) {
        if (point != null)
            return point.getPointValueBefore(time);
        return pointValueDao.getPointValueBefore(dataPointId, time);
    }

    public PointValueTime getPointValueAt(long time) {
        if (point != null)
            return point.getPointValueAt(time);
        return pointValueDao.getPointValueAt(dataPointId, time);
    }

    public PointValueTime getPointValueAfter(long time) {
        if (point != null)
            return point.getPointValueAfter(time);
        return pointValueDao.getPointValueAfter(dataPointId, time);
    }

    public PointValueTime getPointValue() {
        if (point != null)
            return point.getPointValue();
        return pointValueDao.getLatestPointValue(dataPointId);
    }

    //
    //
    // Point values lists
    //
    public List<PointValueTime> getPointValues(long since) {
        if (point != null)
            return point.getPointValues(since);
        return pointValueDao.getPointValues(dataPointId, since);
    }

    public List<PointValueTime> getPointValuesBetween(long from, long to) {
        if (point != null)
            return point.getPointValuesBetween(from, to);
        return pointValueDao.getPointValuesBetween(dataPointId, from, to);
    }

    public List<PointValueTime> getLatestPointValues(int limit) {
        if (point != null)
            return point.getLatestPointValues(limit);
        return pointValueDao.getLatestPointValues(dataPointId, limit);
    }
}
