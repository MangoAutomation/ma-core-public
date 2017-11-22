/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.pair.LongPair;

/**
 * 
 * Class to output query execution times
 * 
 * INFO Level log Output is:
 * start[ts] time[exec ms] tag[functionName(param1vaoue,..,paramNvalue){num results}]
 * 
 * 
 * For Point Value Daos
 * @author Terry Packer
 *
 */
public class PointValueDaoMetrics implements PointValueDao{

	private final PointValueDao dao;
	private final long metricsThreshold;
	
	public PointValueDaoMetrics(PointValueDao dao){
		this.dao = dao;
        this.metricsThreshold = Common.envProps.getLong("db.metricsThreshold", 0L);
	}

	public PointValueDao getBaseDao(){
		return this.dao;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#savePointValueSync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public PointValueTime savePointValueSync(int pointId,
			PointValueTime pointValue, SetPointSource source) {
		return dao.savePointValueSync(pointId, pointValue, source);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#savePointValueAsync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public void savePointValueAsync(int pointId, PointValueTime pointValue,
			SetPointSource source) {
		dao.savePointValueAsync(pointId, pointValue, source);
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValues(int, long)
	 */
	@Override
	public List<PointValueTime> getPointValues(int pointId, long since) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		List<PointValueTime> values = dao.getPointValues(pointId, since);
    	    LogStopWatch.stop("getPointValues(pointId,since) (" + pointId + ", " +since + "){" + values.size() +"}", this.metricsThreshold);
    	    return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long)
	 */
	@Override
	public List<PointValueTime> getPointValuesBetween(int pointId, long from,
			long to) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		List<PointValueTime> values = dao.getPointValuesBetween(pointId, from,to);
		LogStopWatch.stop("getPointValuesBetween(pointId, from, to)  ("+pointId+", "+from+", "+to + "){" + values.size() +"}", this.metricsThreshold);
    	    return values;
	}
	
	/* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long, limit)
     */
    @Override
    public List<PointValueTime> getPointValuesBetween(int pointId, long from,
            long to, int limit) {
        LogStopWatch LogStopWatch = new LogStopWatch();
        List<PointValueTime> values = dao.getPointValuesBetween(pointId, from, to, limit);
        LogStopWatch.stop("getPointValuesBetween(pointId, from, to)  ("+pointId+", "+from+", "+to+ ", "+limit + "){" + values.size() +"}", this.metricsThreshold);
        return values;

    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int pointId, int limit) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		List<PointValueTime> values = dao.getLatestPointValues(pointId, limit);
		LogStopWatch.stop("getLatestPointValues(pointId,limit) (" + pointId + ", " + limit + "){" + values.size() +"}", this.metricsThreshold);
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int, long)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int pointId, int limit,
			long before) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		List<PointValueTime> values = dao.getLatestPointValues(pointId, limit,before);
		LogStopWatch.stop("getLatestPointValues(pointId,limit,before) (" + pointId +", " + limit + ", " + before + "){" + values.size() +"}", this.metricsThreshold);
		return values;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(java.util.List, java.lang.Integer, long, boolean, com.infiniteautomation.mango.db.query.PVTQueryCallback)
	 */
	public void getLatestPointValues(List<Integer> ids, long before, boolean orderById, Integer limit, PVTQueryCallback<IdPointValueTime> callback){
	    LogStopWatch LogStopWatch = new LogStopWatch();
        dao.getLatestPointValues(ids, before, orderById, limit, callback);
        LogStopWatch.stop("getLatestPointValues(pointId,limit,before, orderById, callback) (" + ids +", " + limit + ", " + before + "," + orderById + ", callback)", this.metricsThreshold);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValue(int)
	 */
	@Override
	public PointValueTime getLatestPointValue(int pointId) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		PointValueTime value = dao.getLatestPointValue(pointId);
		LogStopWatch.stop("getLatestPointValue(pointId) (" + pointId + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
		return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueBefore(int, long)
	 */
	@Override
	public PointValueTime getPointValueBefore(int pointId, long time) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		PointValueTime value = dao.getPointValueBefore(pointId,time);
    	    LogStopWatch.stop("getPointValuesBefore(pointId,time) (" + pointId + ", " + time + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
    	    return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAfter(int, long)
	 */
	@Override
	public PointValueTime getPointValueAfter(int pointId, long time) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		PointValueTime value = dao.getPointValueAfter(pointId,time);
		LogStopWatch.stop("getPointValueAfter(pointId,time) (" + pointId + ", " + time + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAt(int, long)
	 */
	@Override
	public PointValueTime getPointValueAt(int pointId, long time) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		PointValueTime value = dao.getPointValueAt(pointId,time);
		LogStopWatch.stop("getPointValueAt(pointId,time) (" + pointId + ", " + time + "){" + (value != null ? 1 : 0) + "}", this.metricsThreshold);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long, com.serotonin.db.MappedRowCallback)
	 */
	@Override
	public void getPointValuesBetween(int pointId, long from, long to,
			MappedRowCallback<PointValueTime> callback) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.getPointValuesBetween(pointId,from,to,callback);
		LogStopWatch.stop("getPointValuesBetween(pointId,from,to,callback) + (" + pointId + ", " + from + ", " + to + ", " + callback.toString() + ")", this.metricsThreshold);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(java.util.List, long, long, com.serotonin.db.MappedRowCallback)
	 */
	@Override
	public void getPointValuesBetween(List<Integer> pointIds, long from,
			long to, MappedRowCallback<IdPointValueTime> callback) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.getPointValuesBetween(pointIds,from,to,callback);
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		LogStopWatch.stop("getPointValuesBetween(pointIds,from,to,callback) ("+ sqlIn + ", " + from + ", " + to + ", " + callback.toString() + ")" , this.metricsThreshold);
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#wideQuery(int, long, long, com.serotonin.db.WideQueryCallback)
	 */
	@Override
	public void wideQuery(int pointId, long from, long to, WideQueryCallback<PointValueTime> callback) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.wideQuery(pointId, from , to, callback);
		LogStopWatch.stop("wideQuery(pointId,from,to,callback) ("+ pointId + ", " + from + ", " + to + ", " + callback.toString() + ")" , this.metricsThreshold);
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBefore(int, long)
	 */
	@Override
	public long deletePointValuesBefore(int pointId, long time) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long value = dao.deletePointValuesBefore(pointId,time);
		LogStopWatch.stop("deletePointValuesBefore(pointId,time) (" + pointId + ", " + time + ")", this.metricsThreshold);
    	return value;

	}
	
	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBeforeWithoutCount(int, long)
	 */
	@Override
	public boolean deletePointValuesBeforeWithoutCount(int pointId, long time){
		LogStopWatch LogStopWatch = new LogStopWatch();
		boolean value = dao.deletePointValuesBeforeWithoutCount(pointId,time);
		LogStopWatch.stop("deletePointValuesBeforeWithoutCount(pointId,time) (" + pointId + ", " + time + ")", this.metricsThreshold);
    	return value;
	}
	
	/*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBetween(int, startTime, endTime)
     */
    @Override
    public long deletePointValuesBetween(int pointId, long startTime, long endTime){
        LogStopWatch LogStopWatch = new LogStopWatch();
        long value = dao.deletePointValuesBetween(pointId,startTime,endTime);
        LogStopWatch.stop("deletePointValuesBetween(pointId,startTime,endTime) (" + pointId + ", " + startTime + ", " + endTime + ")", this.metricsThreshold);
        return value;
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValues(int)
	 */
	@Override
	public long deletePointValues(int pointId) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long value = dao.deletePointValues(pointId);
		LogStopWatch.stop("deletePointValues(pointId) (" + pointId + ")", this.metricsThreshold);
    	return value;
	}

	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesWithoutCount(int)
	 */
	@Override
	public boolean deletePointValuesWithoutCount(int pointId) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		boolean value = dao.deletePointValuesWithoutCount(pointId);
		LogStopWatch.stop("deletePointValuesWithoutCount(pointId) (" + pointId + ")", this.metricsThreshold);
    	return value;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteAllPointData()
	 */
	@Override
	public long deleteAllPointData() {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long value = dao.deleteAllPointData();
		LogStopWatch.stop("deleteAllPointData()", this.metricsThreshold);
    	return value;
	}
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteAllPointDataWithoutCount()
	 */
	@Override
	public void deleteAllPointDataWithoutCount() {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.deleteAllPointDataWithoutCount();
		LogStopWatch.stop("deleteAllPointDataWithoutCount()", this.metricsThreshold);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValues()
	 */
	@Override
	public long deleteOrphanedPointValues() {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long value = dao.deleteOrphanedPointValues();
		LogStopWatch.stop("deleteOrphanedPointValues()", this.metricsThreshold);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValuesWithoutCount()
	 */
	@Override
	public void deleteOrphanedPointValuesWithoutCount() {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.deleteOrphanedPointValuesWithoutCount();
		LogStopWatch.stop("deleteOrphanedPointValuesWithoutCount()", this.metricsThreshold);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValueAnnotations()
	 */
	@Override
	public void deleteOrphanedPointValueAnnotations() {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.deleteOrphanedPointValueAnnotations();
		LogStopWatch.stop("deleteOrphanedPointValueAnnotations()", this.metricsThreshold);
    	return;
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#dateRangeCount(int, long, long)
	 */
	@Override
	public long dateRangeCount(int pointId, long from, long to) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long value = dao.dateRangeCount(pointId, from, to);
		LogStopWatch.stop("dateRangeCount(pointId,from,to) (" + pointId + ", " + from + ", " + to + ")", this.metricsThreshold);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getInceptionDate(int)
	 */
	@Override
	public long getInceptionDate(int pointId) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long value = dao.getInceptionDate(pointId);
		LogStopWatch.stop("getInceptionDate(pointId) (" + pointId + ")", this.metricsThreshold); 
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartTime(java.util.List)
	 */
	@Override
	public long getStartTime(List<Integer> pointIds) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long result = dao.getStartTime(pointIds);

    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		LogStopWatch.stop("getStartTime(pointIds) (" + sqlIn + ")", this.metricsThreshold);
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getEndTime(java.util.List)
	 */
	@Override
	public long getEndTime(List<Integer> pointIds) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long result = dao.getEndTime(pointIds);
		
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
    	LogStopWatch.stop("getEndTime(pointIds) (" + sqlIn + ")", this.metricsThreshold);
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartAndEndTime(java.util.List)
	 */
	@Override
	public LongPair getStartAndEndTime(List<Integer> pointIds) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		LongPair result = dao.getStartAndEndTime(pointIds);
		
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		LogStopWatch.stop("getStartAndEndTime(pointIds) + (" + sqlIn + ")", this.metricsThreshold);
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getFiledataIds(int)
	 */
	@Override
	public List<Long> getFiledataIds(int pointId) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		List<Long> value = dao.getFiledataIds(pointId);
		LogStopWatch.stop("getFiledataIds(pointId) (" + pointId + ")", this.metricsThreshold);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueAsync(int, com.serotonin.m2m2.rt.dataImage.PointValueIdTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public void updatePointValueAsync(int id, PointValueTime pvt, SetPointSource source) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.updatePointValueAsync(id, pvt, source);
		LogStopWatch.stop("updatePointValueAsync(id, ts, source) (" + id + ", pvt)", this.metricsThreshold);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueSync(int, com.serotonin.m2m2.rt.dataImage.PointValueIdTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public PointValueTime updatePointValueSync(int dataPointId, PointValueTime pvt, SetPointSource source) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		PointValueTime value = dao.updatePointValueSync(dataPointId, pvt, source);
		LogStopWatch.stop("updatePointValuesSync(dataPointId, ts, source) (" + dataPointId + ", pvt, source)" , this.metricsThreshold);
    	    return value;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValue(int)
	 */
	@Override
	public long deletePointValue(int dataPointId, long ts) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long value = dao.deletePointValue(dataPointId, ts);
		LogStopWatch.stop("deletePointValue(dataPointId, ts) + (" + dataPointId + ", " + ts + ")", this.metricsThreshold);
    	    return value;
	}

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#wideBookendQuery(int, long, long, java.lang.Integer, com.serotonin.db.WideQueryCallback)
     */
    @Override
    public void wideBookendQuery(int pointId, long from, long to, Integer limit,
            BookendQueryCallback<PointValueTime> callback) {
        LogStopWatch logStopWatch = new LogStopWatch();
        dao.wideBookendQuery(pointId, from, to, limit, callback);
        logStopWatch.stop("wideBookendQuery(dataPointId, from, to, limit, callback) + (" + pointId + ", " + to + ", " + from + ", " + limit + "callback)", this.metricsThreshold);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#wideBookendQuery(java.util.List, long, long, java.lang.Integer, com.serotonin.db.WideQueryCallback)
     */
    @Override
    public void wideBookendQuery(List<Integer> pointIds, long from, long to, boolean orderById, Integer limit,
            BookendQueryCallback<IdPointValueTime> callback) {
        LogStopWatch logStopWatch = new LogStopWatch();
        dao.wideBookendQuery(pointIds, from, to, orderById, limit, callback);
        logStopWatch.stop("wideBookendQuery(dataPointIds, from, to, orderById, limit, callback) + (" + pointIds + ", " + to + ", " + from + ", " + limit + "callback)", this.metricsThreshold);

    }
	
}
