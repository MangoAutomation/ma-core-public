/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueIdTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.pair.LongPair;

/**
 * 
 * Class to output query execution times
 * 
 * For Point Value Daos
 * @author Terry Packer
 *
 */
public class PointValueDaoMetrics implements PointValueDao{

	private static final Log LOG = LogFactory.getLog(PointValueDaoMetrics.class);

	private PointValueDao dao;
	private long startTime;
	private long endTime;
	private long duration;
	private long factor;
	private String suffix;
	
	
	public PointValueDaoMetrics(PointValueDao dao, TimeScale scale){
		this.dao = dao;
		
		switch(scale){
		case NANOSECONDS:
			factor = 1;
			suffix = "ns";
		break;
		case MILLISECONDS:
			factor = 1000000;
			suffix = "ms";
		break;
		case SECONDS:
			factor = 1000000000;
			suffix = "s";
		break;
		}
		
	}
	
	public long getLastDuration(){
		return duration;
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
		startTime = System.nanoTime();
		List<PointValueTime> values = dao.getPointValues(pointId, since);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long)
	 */
	@Override
	public List<PointValueTime> getPointValuesBetween(int pointId, long from,
			long to) {
		startTime = System.nanoTime();
		List<PointValueTime> values = dao.getPointValuesBetween(pointId, from,to);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return values;

	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int pointId, int limit) {
		startTime = System.nanoTime();
		List<PointValueTime> values = dao.getLatestPointValues(pointId, limit);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int, long)
	 */
	@Override
	public List<PointValueTime> getLatestPointValues(int pointId, int limit,
			long before) {
		startTime = System.nanoTime();
		List<PointValueTime> values = dao.getLatestPointValues(pointId, limit,before);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValue(int)
	 */
	@Override
	public PointValueTime getLatestPointValue(int pointId) {
		startTime = System.nanoTime();
		PointValueTime value = dao.getLatestPointValue(pointId);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueBefore(int, long)
	 */
	@Override
	public PointValueTime getPointValueBefore(int pointId, long time) {
		startTime = System.nanoTime();
		PointValueTime value = dao.getPointValueBefore(pointId,time);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAfter(int, long)
	 */
	@Override
	public PointValueTime getPointValueAfter(int pointId, long time) {
		startTime = System.nanoTime();
		PointValueTime value = dao.getPointValueAfter(pointId,time);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAt(int, long)
	 */
	@Override
	public PointValueTime getPointValueAt(int pointId, long time) {
		startTime = System.nanoTime();
		PointValueTime value = dao.getPointValueAt(pointId,time);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long, com.serotonin.db.MappedRowCallback)
	 */
	@Override
	public void getPointValuesBetween(int pointId, long from, long to,
			MappedRowCallback<PointValueTime> callback) {
		startTime = System.nanoTime();
		dao.getPointValuesBetween(pointId,from,to,callback);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);	
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(java.util.List, long, long, com.serotonin.db.MappedRowCallback)
	 */
	@Override
	public void getPointValuesBetween(List<Integer> pointIds, long from,
			long to, MappedRowCallback<IdPointValueTime> callback) {
		startTime = System.nanoTime();
		dao.getPointValuesBetween(pointIds,from,to,callback);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for points: " + sqlIn);	
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBefore(int, long)
	 */
	@Override
	public long deletePointValuesBefore(int pointId, long time) {
		startTime = System.nanoTime();
		long value = dao.deletePointValuesBefore(pointId,time);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;

	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValues(int)
	 */
	@Override
	public long deletePointValues(int pointId) {
		startTime = System.nanoTime();
		long value = dao.deletePointValues(pointId);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteAllPointData()
	 */
	@Override
	public long deleteAllPointData() {
		startTime = System.nanoTime();
		long value = dao.deleteAllPointData();
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for all points.");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValues()
	 */
	@Override
	public long deleteOrphanedPointValues() {
		startTime = System.nanoTime();
		long value = dao.deleteOrphanedPointValues();
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for all points.");
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValueAnnotations()
	 */
	@Override
	public void deleteOrphanedPointValueAnnotations() {
		startTime = System.nanoTime();
		dao.deleteOrphanedPointValueAnnotations();
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for all points.");
    	return;
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#dateRangeCount(int, long, long)
	 */
	@Override
	public long dateRangeCount(int pointId, long from, long to) {
		startTime = System.nanoTime();
		long value = dao.dateRangeCount(pointId, from, to);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getInceptionDate(int)
	 */
	@Override
	public long getInceptionDate(int pointId) {
		startTime = System.nanoTime();
		long value = dao.getInceptionDate(pointId);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartTime(java.util.List)
	 */
	@Override
	public long getStartTime(List<Integer> pointIds) {
		startTime = System.nanoTime();
		long result = dao.getStartTime(pointIds);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for points: " + sqlIn);	
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getEndTime(java.util.List)
	 */
	@Override
	public long getEndTime(List<Integer> pointIds) {
		startTime = System.nanoTime();
		long result = dao.getEndTime(pointIds);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for points: " + sqlIn);	
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartAndEndTime(java.util.List)
	 */
	@Override
	public LongPair getStartAndEndTime(List<Integer> pointIds) {
		startTime = System.nanoTime();
		LongPair result = dao.getStartAndEndTime(pointIds);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	String sqlIn = "[";
		for(int i=0; i<pointIds.size(); i++){
			sqlIn += pointIds.get(i);
			if(i < pointIds.size())
				sqlIn += ",";
		}
		sqlIn += "]";
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for points: " + sqlIn);	
    	return result;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getFiledataIds(int)
	 */
	@Override
	public List<Long> getFiledataIds(int pointId) {
		startTime = System.nanoTime();
		List<Long> value = dao.getFiledataIds(pointId);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesWithMismatchedType(int, int)
	 */
	@Override
	public long deletePointValuesWithMismatchedType(int id, int dataTypeId) {
		startTime = System.nanoTime();
		long value = dao.deletePointValuesWithMismatchedType(id,dataTypeId);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + id);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueAsync(int, com.serotonin.m2m2.rt.dataImage.PointValueIdTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public void updatePointValueAsync(int id, PointValueIdTime pvt,
			SetPointSource source) {
		startTime = System.nanoTime();
		dao.updatePointValueAsync(id,pvt,source);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + id);
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueSync(int, com.serotonin.m2m2.rt.dataImage.PointValueIdTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
	 */
	@Override
	public PointValueIdTime updatePointValueSync(int dataPointId,
			PointValueIdTime pvt, SetPointSource source) {
		startTime = System.nanoTime();
		PointValueIdTime value = dao.updatePointValueSync(dataPointId, pvt, source);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + dataPointId);
    	return value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesWithIdsBetween(int, long, long, com.serotonin.db.MappedRowCallback)
	 */
	@Override
	public void getPointValuesWithIdsBetween(int pointId, long from, long to,
			MappedRowCallback<PointValueIdTime> callback) {
		startTime = System.nanoTime();
		dao.getPointValuesWithIdsBetween(pointId, from, to,callback);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point with id: " + pointId);
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValue(int)
	 */
	@Override
	public long deletePointValue(int pointValueId) {
		startTime = System.nanoTime();
		long value = dao.deletePointValue(pointValueId);
		endTime = System.nanoTime();
    	duration = endTime - startTime;
    	LOG.info("Query time: " + (double)duration/(double)factor + suffix + " for point value with id: " + pointValueId);
    	return value;
	}

	public enum TimeScale{
		NANOSECONDS, MILLISECONDS,SECONDS
	}
	
	
	
}
