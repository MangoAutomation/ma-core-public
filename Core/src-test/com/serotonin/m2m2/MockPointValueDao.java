/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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

    @Override
    public void savePointValueAsync(int pointId, PointValueTime pointValue, SetPointSource source, Consumer<Long> savedCallback) {
        savePointValueSync(pointId, pointValue, source);
        if(savedCallback != null)
            savedCallback.accept(pointValue.getTime());
    }

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
    
    @Override
    public void getLatestPointValues(int pointId, long before, Integer limit, final PVTQueryCallback<PointValueTime> callback) {
        List<PointValueTime> existing = data.get(pointId);
        int count = 0;
        if(existing != null) {
            for(int i=existing.size() -1; i>=0; i--) {
                PointValueTime pvt = existing.get(i);
                if(pvt.getTime() < before) {
                    try {
                        callback.row(pvt, count);
                    } catch (IOException e) {
                        break;
                    }
                    count++;
                }
                if(count >= limit)
                    break;
            }
        }
    }

    @Override
    public PointValueTime getLatestPointValue(int pointId) {
        List<PointValueTime> existing = data.get(pointId);
        if(existing != null) {
            return existing.get(existing.size() -1);
        }
        return null;
    }

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

    @Override
    public void getPointValuesBetween(int pointId, long from, long to,
            MappedRowCallback<PointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getPointValuesBetween(List<Integer> pointIds, long from, long to,
            MappedRowCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wideQuery(int pointId, long from, long to,
            WideQueryCallback<PointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long deletePointValuesBetween(int pointId, long startTime, long endTime) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long deletePointValuesBefore(int pointId, long time) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean deletePointValuesBeforeWithoutCount(int pointId, long time) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long deletePointValues(int pointId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean deletePointValuesWithoutCount(int pointId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long deleteAllPointData() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteAllPointDataWithoutCount() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long deleteOrphanedPointValues() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteOrphanedPointValuesWithoutCount() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteOrphanedPointValueAnnotations() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long dateRangeCount(int pointId, long from, long to) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getInceptionDate(int pointId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getStartTime(List<Integer> pointIds) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getEndTime(List<Integer> pointIds) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public LongPair getStartAndEndTime(List<Integer> pointIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getFiledataIds(int pointId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updatePointValueAsync(int dataPointId, PointValueTime pvt, SetPointSource source) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public PointValueTime updatePointValueSync(int dataPointId, PointValueTime pvt,
            SetPointSource source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long deletePointValue(int dataPointId, long ts) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void wideBookendQuery(List<Integer> pointIds, long from, long to, boolean orderById, Integer limit,
            BookendQueryCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getLatestPointValues(List<Integer> ids, long before, boolean orderById, Integer limit, final PVTQueryCallback<IdPointValueTime> callback) {
        
    }

    @Override
    public void getPointValuesBetween(List<Integer> ids, long from, long to, boolean orderById,
            Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub
        
    }

}
