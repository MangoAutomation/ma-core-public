/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.pair.LongPair;

/**
 *
 * @author Terry Packer
 */
public class MockPointValueDao implements PointValueDao{

    protected List<PointValueTime> data = new ArrayList<PointValueTime>();
    
    public List<PointValueTime> getData() {
        return this.data;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#savePointValueSync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public PointValueTime savePointValueSync(int pointId, PointValueTime pointValue,
            SetPointSource source) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#savePointValueAsync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public void savePointValueAsync(int pointId, PointValueTime pointValue, SetPointSource source) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValues(int, long)
     */
    @Override
    public List<PointValueTime> getPointValues(int pointId, long since) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long)
     */
    @Override
    public List<PointValueTime> getPointValuesBetween(int pointId, long from, long to) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long, int)
     */
    @Override
    public List<PointValueTime> getPointValuesBetween(int pointId, long from, long to, int limit) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int)
     */
    @Override
    public List<PointValueTime> getLatestPointValues(int pointId, int limit) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int, long)
     */
    @Override
    public List<PointValueTime> getLatestPointValues(int pointId, int limit, long before) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValue(int)
     */
    @Override
    public PointValueTime getLatestPointValue(int pointId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueBefore(int, long)
     */
    @Override
    public PointValueTime getPointValueBefore(int pointId, long time) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAfter(int, long)
     */
    @Override
    public PointValueTime getPointValueAfter(int pointId, long time) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAt(int, long)
     */
    @Override
    public PointValueTime getPointValueAt(int pointId, long time) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long, com.serotonin.db.MappedRowCallback)
     */
    @Override
    public void getPointValuesBetween(int pointId, long from, long to,
            MappedRowCallback<PointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(java.util.List, long, long, com.serotonin.db.MappedRowCallback)
     */
    @Override
    public void getPointValuesBetween(List<Integer> pointIds, long from, long to,
            MappedRowCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#wideQuery(int, long, long, com.serotonin.db.WideQueryCallback)
     */
    @Override
    public void wideQuery(int pointId, long from, long to,
            WideQueryCallback<PointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBetween(int, long, long)
     */
    @Override
    public long deletePointValuesBetween(int pointId, long startTime, long endTime) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBefore(int, long)
     */
    @Override
    public long deletePointValuesBefore(int pointId, long time) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesBeforeWithoutCount(int, long)
     */
    @Override
    public boolean deletePointValuesBeforeWithoutCount(int pointId, long time) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValues(int)
     */
    @Override
    public long deletePointValues(int pointId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValuesWithoutCount(int)
     */
    @Override
    public boolean deletePointValuesWithoutCount(int pointId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteAllPointData()
     */
    @Override
    public long deleteAllPointData() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteAllPointDataWithoutCount()
     */
    @Override
    public void deleteAllPointDataWithoutCount() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValues()
     */
    @Override
    public long deleteOrphanedPointValues() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValuesWithoutCount()
     */
    @Override
    public void deleteOrphanedPointValuesWithoutCount() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deleteOrphanedPointValueAnnotations()
     */
    @Override
    public void deleteOrphanedPointValueAnnotations() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#dateRangeCount(int, long, long)
     */
    @Override
    public long dateRangeCount(int pointId, long from, long to) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getInceptionDate(int)
     */
    @Override
    public long getInceptionDate(int pointId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartTime(java.util.List)
     */
    @Override
    public long getStartTime(List<Integer> pointIds) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getEndTime(java.util.List)
     */
    @Override
    public long getEndTime(List<Integer> pointIds) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getStartAndEndTime(java.util.List)
     */
    @Override
    public LongPair getStartAndEndTime(List<Integer> pointIds) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getFiledataIds(int)
     */
    @Override
    public List<Long> getFiledataIds(int pointId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueAsync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public void updatePointValueAsync(int dataPointId, PointValueTime pvt, SetPointSource source) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#updatePointValueSync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public PointValueTime updatePointValueSync(int dataPointId, PointValueTime pvt,
            SetPointSource source) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#deletePointValue(int, long)
     */
    @Override
    public long deletePointValue(int dataPointId, long ts) {
        // TODO Auto-generated method stub
        return 0;
    }

}
