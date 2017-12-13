/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
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

    /**
     * Gets point values since a certain time, can add an initial value and final value to the returned
     * results. Useful for creating graphs.
     * @param since
     * 			epoch time in ms
     * @param insertInitial
     * 			fetch the previous point value before 'since', and insert it at time 'since' 
     * @param insertFinal
     * 			take the last point value from the list and insert it at the current time
     * @return
     */
    public List<PointValueTime> getPointValues(long since, boolean insertInitial, boolean insertFinal) {
        List<PointValueTime> list = getPointValues(since);

        if (insertInitial) {
            PointValueTime prevValue = getPointValueBefore(since);

            if (prevValue != null) {
                // don't insert the initial value if it already exists
                if (list.isEmpty() || list.get(0).getTime() != since) {
                    list.add(0, new PointValueTime(prevValue.getValue(), since));
                }
            }
        }

        if (insertFinal && !list.isEmpty()) {
            PointValueTime finalValue = list.get(list.size() - 1);
            long endTime = Common.timer.currentTimeMillis();

            // don't insert the final value if it already exists
            if (finalValue != null && finalValue.getTime() != endTime) {
                list.add(new PointValueTime(finalValue.getValue(), endTime));
            }
        }
        
        return list;
    }
    
    public List<PointValueTime> getPointValuesBetween(long from, long to) {
        if ((point != null) && (useCache))
            return point.getPointValuesBetween(from, to);
        return pointValueDao.getPointValuesBetween(dataPointId, from, to);
    }

    /**
     * Gets point values in a certain time period, can add an initial value and final value to the returned
     * results. Useful for creating graphs.
     * @param from
     * 			epoch time in ms
     * @param to
     * 			epoch time in ms
     * @param insertInitial
     * 			fetch the previous point value before 'from', and insert it at time 'from' 
     * @param insertFinal
     * 			take the last point value from the list and insert it at time 'to'
     * @return
     */
    public List<PointValueTime> getPointValuesBetween(long from, long to, boolean insertInitial, boolean insertFinal) {
        List<PointValueTime> values = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        ids.add(dataPointId);
        pointValueDao.wideBookendQuery(ids, from, to, false, null, new BookendQueryCallback<IdPointValueTime>() {

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend) throws IOException {
                if(insertInitial)
                    values.add(value);
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                values.add(value);
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend) throws IOException {
                if(insertFinal)
                    values.add(value);
            }
        });
        return values;
    }

    public List<PointValueTime> getLatestPointValues(int limit) {
        	if ((point != null)&&(useCache))
        	    return point.getLatestPointValues(limit);
        return pointValueDao.getLatestPointValues(dataPointId, limit);
    }
}
