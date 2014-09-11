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
    private boolean useCache;
    
    public PointValueFacade(int dataPointId, boolean useCache) {
        this.dataPointId = dataPointId;
        point = Common.runtimeManager.getDataPoint(dataPointId);
        pointValueDao = Common.databaseProxy.newPointValueDao();
        this.useCache = useCache;
    }

    public PointValueFacade(int dataPointId) {
        this.dataPointId = dataPointId;
        point = Common.runtimeManager.getDataPoint(dataPointId);
        pointValueDao = Common.databaseProxy.newPointValueDao();
        this.useCache = true;
    }
    
    //
    //
    // Single point value
    //
    public PointValueTime getPointValueBefore(long time) {
        if ((point != null)&&(useCache))
            return point.getPointValueBefore(time);
        return pointValueDao.getPointValueBefore(dataPointId, time);
    }

    public PointValueTime getPointValueAt(long time) {
    	if ((point != null)&&(useCache))
            return point.getPointValueAt(time);
        return pointValueDao.getPointValueAt(dataPointId, time);
    }

    public PointValueTime getPointValueAfter(long time) {
    	if ((point != null)&&(useCache))
            return point.getPointValueAfter(time);
        return pointValueDao.getPointValueAfter(dataPointId, time);
    }

    public PointValueTime getPointValue() {
    	if ((point != null)&&(useCache))
            return point.getPointValue();
        return pointValueDao.getLatestPointValue(dataPointId);
    }

    //
    //
    // Point values lists
    //
    public List<PointValueTime> getPointValues(long since) {
    	if ((point != null)&&(useCache))
            return point.getPointValues(since);
        return pointValueDao.getPointValues(dataPointId, since);
    }

    public List<PointValueTime> getPointValues(long since, boolean insertInitial, boolean insertFinal) {
        List<PointValueTime> list;
        
        if (point != null) {
            list = point.getPointValues(since);
        }
        else {
            list = pointValueDao.getPointValues(dataPointId, since);
        }
        
        if (insertInitial && !(list.size() > 0 && list.get(0).getTime() == since)) {
            PointValueTime prevValue = getPointValueBefore(since);
            if (prevValue != null) {
                PointValueTime initialValue = new PointValueTime(prevValue.getValue(), since);
                list.add(0, initialValue);
            }
        }
        
        if (insertFinal && !list.isEmpty()) {
            PointValueTime finalValue = list.get(list.size()-1);
            if (finalValue != null)
                list.add(new PointValueTime(finalValue.getValue(), System.currentTimeMillis()));
        }
        
        return list;
    }
    
    public List<PointValueTime> getPointValuesBetween(long from, long to) {
    	if ((point != null)&&(useCache))
            return point.getPointValuesBetween(from, to);
        return pointValueDao.getPointValuesBetween(dataPointId, from, to);
    }

    public List<PointValueTime> getPointValuesBetween(long from, long to, boolean insertInitial, boolean insertFinal) {
        List<PointValueTime> list;
        
        if (point != null) {
            list = point.getPointValuesBetween(from, to);
        }
        else {
            list = pointValueDao.getPointValuesBetween(dataPointId, from, to);
        }
        
        if (insertInitial && !(list.size() > 0 && list.get(0).getTime() == from)) {
            PointValueTime prevValue = getPointValueBefore(from);
            if (prevValue != null) {
                PointValueTime initialValue = new PointValueTime(prevValue.getValue(), from);
                list.add(0, initialValue);
            }
        }
        
        if (insertFinal && !list.isEmpty()) {
            long endTime = to <= System.currentTimeMillis() ? to : System.currentTimeMillis();
            PointValueTime finalValue = list.get(list.size()-1);
            if (finalValue != null)
                list.add(new PointValueTime(finalValue.getValue(), endTime));
        }
        
        return list;
    }

    public List<PointValueTime> getLatestPointValues(int limit) {
    	if ((point != null)&&(useCache))
            return point.getLatestPointValues(limit);
        return pointValueDao.getLatestPointValues(dataPointId, limit);
    }
}
