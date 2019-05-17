/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

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

    /**
     * Get the value at or just after this time
     * @param time
     * @return
     */
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
        List<PointValueTime> cache = buildCacheView(from, to);
        
        pointValueDao.wideBookendQuery(ids, from, to, false, null, new BookendQueryCallback<IdPointValueTime>() {

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend) throws IOException {
                //If there is no value before from the bookend value.value will be null with a value.timestamp == from
                if(insertInitial) {
                    if(cache != null) {
                        processRow((PointValueTime)value, index, bookend, false, cache, values);
                    }else {
                        values.add(value);
                    }
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                if(cache != null) {
                    processRow((PointValueTime)value, index, false, false, cache, values);
                }else {
                    values.add(value);
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend) throws IOException {
                if(insertFinal) {
                    if(cache != null) {
                        processRow((PointValueTime)value, index, bookend, false, cache, values);
                    }else {
                        values.add(value);
                    }
                }
            }
        });
        return values;
    }

    /**
     * Write out any cached values that would be equal to or between the time of the incomming 
     *   point value and the next one to be returned by the query.
     *   this should be called before processing this value
     * @param value
     * @param bookend
     * @return true to continue to process the incoming value, false if it was a bookend that was replaced via the cache
     * @throws IOException 
     */
    protected boolean processValueThroughCache(PointValueTime value, int index, boolean bookend, List<PointValueTime> pointCache, List<PointValueTime> values) throws IOException {
        if(pointCache != null && pointCache.size() > 0) {
            ListIterator<PointValueTime> it = pointCache.listIterator();
            while(it.hasNext()) {
                PointValueTime pvt = it.next();
                if(pvt.getTime() > value.getTime()) {
                    //Can't be a bookend
                    processRow(pvt, index, false, true, pointCache, values);
                    it.remove();
                }else if(pvt.getTime() == value.getTime()) {
                    //Could be a bookend
                    processRow(pvt, index, bookend, true, pointCache, values);
                    it.remove();
                    return false;
                }else
                    break; //No more since we are in time order of the query
            }
        }
        return true;
    }
    
    /**
     * Common row processing logic
     * @param value
     * @param index
     * @param bookend
     * @throws IOException
     */
    protected void processRow(PointValueTime value, int index, boolean bookend, boolean cached, List<PointValueTime> pointCache, List<PointValueTime> values) throws IOException {
        if(pointCache != null && !cached)
            if(!processValueThroughCache(value, index, bookend, pointCache, values))
                return;
        
        //Add this value
        values.add(value);
    }
    
    public List<PointValueTime> getLatestPointValues(int limit) {
        	if ((point != null)&&(useCache))
        	    return point.getLatestPointValues(limit);
        return pointValueDao.getLatestPointValues(dataPointId, limit);
    }
    
    /**
     * Return a view of the cache if there are points within the range, if the facade is not to use cache return null 
     * @param from
     * @param to
     * @return cacheView or null if not allowed to use cache
     */
    private List<PointValueTime> buildCacheView(long from, long to) {
        if ((point != null) && (useCache)) {
            List<PointValueTime> cache = point.getCacheCopy();
            List<PointValueTime> pointCache = new ArrayList<>(cache.size());
            for(PointValueTime pvt : cache) {
                if(pvt.getTime() >= from && pvt.getTime() < to) {
                    pointCache.add(pvt);
                }
            }
            if(!pointCache.isEmpty())
                Collections.sort(pointCache);
            return pointCache;
        }else
            return null;
    }
}
