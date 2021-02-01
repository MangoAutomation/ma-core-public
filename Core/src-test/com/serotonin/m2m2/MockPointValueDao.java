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
import java.util.function.Consumer;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;
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
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue,
            SetPointSource source) {
        List<PointValueTime> pvts = data.get(vo.getId());
        if(pvts == null) {
            pvts = new ArrayList<>();
            data.put(vo.getId(), pvts);
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
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue, SetPointSource source) {
        savePointValueSync(vo, pointValue, source);
    }

    @Override
    public List<PointValueTime> getPointValues(DataPointVO vo, long since) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() >= since)
                    pvts.add(pvt);
            }
        }
        return pvts;
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() >= from && pvt.getTime() < to)
                    pvts.add(pvt);
            }
        }
        return pvts;
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to, int limit) {

        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(vo.getId());
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
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(vo.getId());
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
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit, long before) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(vo.getId());
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
    public PointValueTime getLatestPointValue(DataPointVO vo) {
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            return existing.get(existing.size() -1);
        }
        return null;
    }

    @Override
    public PointValueTime getPointValueBefore(DataPointVO vo, long time) {
        List<PointValueTime> existing = data.get(vo.getId());
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
    public PointValueTime getPointValueAfter(DataPointVO vo, long time) {
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() > time)
                    return pvt;
            }
        }
        return null;
    }

    @Override
    public PointValueTime getPointValueAt(DataPointVO vo, long time) {
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() == time)
                    return pvt;
            }
        }
        return null;
    }

    @Override
    public void getPointValuesBetween(DataPointVO vo, long from, long to,
            Consumer<PointValueTime> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void getPointValuesBetween(List<DataPointVO> vos, long from, long to,
            Consumer<IdPointValueTime> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void wideQuery(DataPointVO vo, long from, long to,
            WideQueryCallback<PointValueTime> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public long deletePointValuesBetween(DataPointVO vo, long startTime, long endTime) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long deletePointValuesBefore(DataPointVO vo, long time) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean deletePointValuesBeforeWithoutCount(DataPointVO vo, long time) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long deletePointValues(DataPointVO vo) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean deletePointValuesWithoutCount(DataPointVO vo) {
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
    public long dateRangeCount(DataPointVO vo, long from, long to) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getInceptionDate(DataPointVO vo) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getStartTime(List<DataPointVO> vos) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getEndTime(List<DataPointVO> vos) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public LongPair getStartAndEndTime(List<DataPointVO> vos) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getFiledataIds(DataPointVO vo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long deletePointValue(DataPointVO vo, long ts) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void wideBookendQuery(List<DataPointVO> vos, long from, long to, boolean orderById, Integer limit,
            BookendQueryCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void getLatestPointValues(List<DataPointVO> vos, long before, boolean orderById, Integer limit, final PVTQueryCallback<IdPointValueTime> callback) {

    }

    @Override
    public void getPointValuesBetween(List<DataPointVO> vos, long from, long to, boolean orderById,
            Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
        // TODO Auto-generated method stub

    }

}
