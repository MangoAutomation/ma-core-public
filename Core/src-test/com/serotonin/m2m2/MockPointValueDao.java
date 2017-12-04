/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.pair.LongPair;

/**
 *
 * @author Terry Packer
 */
public class MockPointValueDao implements PointValueDao{

    protected Map<Integer, List<PointValueTime>> data = new ConcurrentHashMap<Integer, List<PointValueTime>>();
    
    public Map<Integer, List<PointValueTime>> getData() {
        return this.data;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#savePointValueSync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public PointValueTime savePointValueSync(int pointId, PointValueTime pointValue,
            SetPointSource source) {
        List<PointValueTime> pvts = data.get(pointId);
        if(pvts == null) {
            pvts = new ArrayList<>();
            data.put(pointId, pvts);
        }
        
        PointValueTime newPvt = null;
        if(source != null)
            newPvt = new AnnotatedPointValueTime(pointValue.getValue(), pointValue.getTime(), source.getSetPointSourceMessage());
        else
            newPvt = new PointValueTime(pointValue.getValue(), pointValue.getTime());
        pvts.add(newPvt);
        
        //Keep in time order?
        Collections.sort(pvts);
        
        return newPvt;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#savePointValueAsync(int, com.serotonin.m2m2.rt.dataImage.PointValueTime, com.serotonin.m2m2.rt.dataImage.SetPointSource)
     */
    @Override
    public void savePointValueAsync(int pointId, PointValueTime pointValue, SetPointSource source) {
        savePointValueSync(pointId, pointValue, source);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValues(int, long)
     */
    @Override
    public List<PointValueTime> getPointValues(int pointId, long since) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() >= since)
                    pvts.add(pvt);
            }
        }
        return pvts;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long)
     */
    @Override
    public List<PointValueTime> getPointValuesBetween(int pointId, long from, long to) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() >= from && pvt.getTime() < to)
                    pvts.add(pvt);
            }
        }
        return pvts;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(int, long, long, int)
     */
    @Override
    public List<PointValueTime> getPointValuesBetween(int pointId, long from, long to, int limit) {
        
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() >= from && pvt.getTime() < to)
                    pvts.add(pvt);
                if(pvts.size() >= limit)
                    break;
            }
        }
        return pvts;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int)
     */
    @Override
    public List<PointValueTime> getLatestPointValues(int pointId, int limit) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                pvts.add(pvt);
                if(pvts.size() >= limit)
                    break;
            }
        }
        return pvts;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(int, int, long)
     */
    @Override
    public List<PointValueTime> getLatestPointValues(int pointId, int limit, long before) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(int i=existing.size() -1; i>=0; i--) {
                PointValueTime pvt = existing.get(i);
                if(pvt.getTime() < before)
                    pvts.add(pvt);
                if(pvts.size() >= limit)
                    break;
            }
        }
        return pvts;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValue(int)
     */
    @Override
    public PointValueTime getLatestPointValue(int pointId) {
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            return existing.get(existing.size() -1);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueBefore(int, long)
     */
    @Override
    public PointValueTime getPointValueBefore(int pointId, long time) {
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(int i=existing.size() -1; i>=0; i--) {
                PointValueTime pvt = existing.get(i);
                if(pvt.getTime() < time)
                    return pvt;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAfter(int, long)
     */
    @Override
    public PointValueTime getPointValueAfter(int pointId, long time) {
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() > time)
                    return pvt;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValueAt(int, long)
     */
    @Override
    public PointValueTime getPointValueAt(int pointId, long time) {
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() == time)
                    return pvt;
            }
        }
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#wideBookendQuery(java.util.List, long, long, java.lang.Integer, com.serotonin.db.WideQueryCallback)
     */
    @Override
    public void wideBookendQuery(List<Integer> pointIds, long from, long to, boolean orderById, Integer limit,
            BookendQueryCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }
    
    /*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getLatestPointValues(java.util.List, long, boolean, java.lang.Integer, com.infiniteautomation.mango.db.query.PVTQueryCallback)
     */
    @Override
    public void getLatestPointValues(List<Integer> ids, long before, boolean orderById, Integer limit, final PVTQueryCallback<IdPointValueTime> callback) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.PointValueDao#getPointValuesBetween(java.util.List, long, long, boolean, java.lang.Integer, com.infiniteautomation.mango.db.query.PVTQueryCallback)
     */
    @Override
    public void getPointValuesBetween(List<Integer> ids, long from, long to, boolean orderById,
            Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

}
