/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.serotonin.db.MappedRowCallback;
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

	private PointValueDao dao;
	
	public PointValueDaoMetrics(PointValueDao dao){
		this.dao = dao;
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
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		List<PointValueTime> values = dao.getPointValues(pointId, since);
    	stopWatch.stop("getPointValues(pointId,since) (" + pointId + ", " +since + "){" + values.size() +"}");
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long)
	 */
	@Override
	public List<PointValueTime> getPointValuesBetween(int pointId, long from,
			long to) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		List<PointValueTime> values = dao.getPointValuesBetween(pointId, from,to);
    	stopWatch.stop("getPointValuesBetween(pointId, from, to)  ("+pointId+", "+from+", "+ to + "){" + values.size() +"}");
    	return values;

	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int pointId, int limit) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		List<PointValueTime> values = dao.getLatestPointValues(pointId, limit);
		stopWatch.stop("getLatestPointValues(pointId,limit) (" + pointId + ", " + limit + "){" + values.size() +"}");
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int, long)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int pointId, int limit,
			long before) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		List<PointValueTime> values = dao.getLatestPointValues(pointId, limit,before);
		stopWatch.stop("getLatestPointValues(pointId,limit,before) (" + pointId +", " + limit + ", " + before + "){" + values.size() +"}");
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValue(int)
	 */
	@Override
	public PointValueTime getLatestPointValue(int pointId) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		PointValueTime value = dao.getLatestPointValue(pointId);
		stopWatch.stop("getLatestPointValue(pointId) (" + pointId + "){" + (value != null ? 1 : 0) + "}");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueBefore(int, long)
	 */
	@Override
	public PointValueTime getPointValueBefore(int pointId, long time) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		PointValueTime value = dao.getPointValueBefore(pointId,time);
    	stopWatch.stop("getPointValuesBefore(pointId,time) (" + pointId + ", " + time + "){" + (value != null ? 1 : 0) + "}");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAfter(int, long)
	 */
	@Override
	public PointValueTime getPointValueAfter(int pointId, long time) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		PointValueTime value = dao.getPointValueAfter(pointId,time);
		stopWatch.stop("getPointValueAfter(pointId,time) (" + pointId + ", " + time + "){" + (value != null ? 1 : 0) + "}");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAt(int, long)
	 */
	@Override
	public PointValueTime getPointValueAt(int pointId, long time) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		PointValueTime value = dao.getPointValueAt(pointId,time);
		stopWatch.stop("getPointValueAt(pointId,time) (" + pointId + ", " + time + "){" + (value != null ? 1 : 0) + "}");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long, com.serotonin.db.MappedRowCallback)
	 */
	@Override
	public void getPointValuesBetween(int pointId, long from, long to,
			MappedRowCallback<PointValueTime> callback) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		dao.getPointValuesBetween(pointId,from,to,callback);
		stopWatch.stop("getPointValuesBetween(pointId,from,to,callback) + (" + pointId + ", " + from + ", " + to + ", " + callback.toString() + ")");
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(java.util.List, long, long, com.serotonin.db.MappedRowCallback)
	 */
	@Override
	public void getPointValuesBetween(List<Integer> pointIds, long from,
			long to, MappedRowCallback<IdPointValueTime> callback) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		dao.getPointValuesBetween(pointIds,from,to,callback);
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		stopWatch.stop("getPointValuesBetween(pointIds,from,to,callback) ("+ sqlIn + ", " + from + ", " + to + ", " + callback.toString() + ")" );
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBefore(int, long)
	 */
	@Override
	public long deletePointValuesBefore(int pointId, long time) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.deletePointValuesBefore(pointId,time);
		stopWatch.stop("deletePointValuesBefore(pointId,time) (" + pointId + ", " + time + ")");
    	return value;

	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValues(int)
	 */
	@Override
	public long deletePointValues(int pointId) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.deletePointValues(pointId);
		stopWatch.stop("deletePointValues(pointId) (" + pointId + ")");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteAllPointData()
	 */
	@Override
	public long deleteAllPointData() {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.deleteAllPointData();
		stopWatch.stop("deleteAllPointData()");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValues()
	 */
	@Override
	public long deleteOrphanedPointValues() {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.deleteOrphanedPointValues();
		stopWatch.stop("deleteOrphanedPointValues()");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValueAnnotations()
	 */
	@Override
	public void deleteOrphanedPointValueAnnotations() {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		dao.deleteOrphanedPointValueAnnotations();
		stopWatch.stop("deleteOrphanedPointValueAnnotations()");
    	return;
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#dateRangeCount(int, long, long)
	 */
	@Override
	public long dateRangeCount(int pointId, long from, long to) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.dateRangeCount(pointId, from, to);
		stopWatch.stop("dateRangeCount(pointId,from,to) (" + pointId + ", " + from + ", " + to + ")");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getInceptionDate(int)
	 */
	@Override
	public long getInceptionDate(int pointId) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.getInceptionDate(pointId);
		stopWatch.stop("getInceptionDate(pointId) (" + pointId + ")"); 
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartTime(java.util.List)
	 */
	@Override
	public long getStartTime(List<Integer> pointIds) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long result = dao.getStartTime(pointIds);

    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		stopWatch.stop("getStartTime(pointIds) (" + sqlIn + ")");
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getEndTime(java.util.List)
	 */
	@Override
	public long getEndTime(List<Integer> pointIds) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long result = dao.getEndTime(pointIds);
		
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
    	stopWatch.stop("getEndTime(pointIds) (" + sqlIn + ")");
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartAndEndTime(java.util.List)
	 */
	@Override
	public LongPair getStartAndEndTime(List<Integer> pointIds) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		LongPair result = dao.getStartAndEndTime(pointIds);
		
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		stopWatch.stop("getStartAndEndTime(pointIds) + (" + sqlIn + ")");
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getFiledataIds(int)
	 */
	@Override
	public List<Long> getFiledataIds(int pointId) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		List<Long> value = dao.getFiledataIds(pointId);
		stopWatch.stop("getFiledataIds(pointId) (" + pointId + ")");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesWithMismatchedType(int, int)
	 */
	@Override
	public long deletePointValuesWithMismatchedType(int id, int dataTypeId) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.deletePointValuesWithMismatchedType(id,dataTypeId);
		stopWatch.stop("deletePointValuesWithMismatchedType(id,dataTypeId) (" + id + ", " + dataTypeId + ")");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueAsync(int, com.serotonin.m2m2.rt.dataImage.PointValueIdTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public void updatePointValueAsync(int id, PointValueTime pvt, SetPointSource source) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		dao.updatePointValueAsync(id, pvt, source);
		stopWatch.stop("updatePointValueAsync(id, ts, source) (" + id + ", pvt)");
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueSync(int, com.serotonin.m2m2.rt.dataImage.PointValueIdTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public PointValueTime updatePointValueSync(int dataPointId, PointValueTime pvt, SetPointSource source) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		PointValueTime value = dao.updatePointValueSync(dataPointId, pvt, source);
		stopWatch.stop("updatePointValuesSync(dataPointId, ts, source) (" + dataPointId + ", pvt, source)" );
    	return value;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValue(int)
	 */
	@Override
	public long deletePointValue(int dataPointId, long ts) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long value = dao.deletePointValue(dataPointId, ts);
		stopWatch.stop("deletePointValue(dataPointId, ts) + (" + dataPointId + ", " + ts + ")");
    	return value;
	}

	
}
