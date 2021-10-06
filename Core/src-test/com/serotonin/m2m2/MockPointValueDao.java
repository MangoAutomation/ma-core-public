/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.db.query.WideCallback;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;

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
        List<PointValueTime> pvts = data.computeIfAbsent(vo.getId(), k -> new ArrayList<>());

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
    public List<PointValueTime> getPointValues(DataPointVO vo, long from) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() >= from)
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
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, long to, int limit) {
        List<PointValueTime> pvts = new ArrayList<>();
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(int i=existing.size() -1; i>=0; i--) {
                PointValueTime pvt = existing.get(i);
                if(pvt.getTime() < to)
                    pvts.add(pvt);
                if(pvts.size() >= limit)
                    break;
            }
        }
        return pvts;
    }

    @Override
    public Optional<PointValueTime> getLatestPointValue(DataPointVO vo) {
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            return Optional.of(existing.get(existing.size() -1));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PointValueTime> getPointValueBefore(DataPointVO vo, long time) {
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(int i=existing.size() -1; i>=0; i--) {
                PointValueTime pvt = existing.get(i);
                if(pvt.getTime() < time)
                    return Optional.of(pvt);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PointValueTime> getPointValueAfter(DataPointVO vo, long time) {
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() > time)
                    return Optional.of(pvt);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PointValueTime> getPointValueAt(DataPointVO vo, long time) {
        List<PointValueTime> existing = data.get(vo.getId());
        if(existing != null) {
            for(PointValueTime pvt : existing) {
                if(pvt.getTime() == time)
                    return Optional.of(pvt);
            }
        }
        return Optional.empty();
    }

    @Override
    public void getPointValuesBetween(DataPointVO vo, long from, long to,
                                      Consumer<? super PointValueTime> callback) {

    }

    @Override
    public void getPointValuesBetween(Collection<? extends DataPointVO> vos, long from, long to,
                                      Consumer<? super IdPointValueTime> callback) {

    }

    @Override
    public void wideQuery(DataPointVO vo, long from, long to,
                          WideCallback<? super PointValueTime> callback) {

    }

    @Override
    public Optional<Long> deletePointValuesBetween(DataPointVO vo, @Nullable Long startTime, @Nullable Long endTime) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> deletePointValuesBefore(DataPointVO vo, long endTime) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> deletePointValues(DataPointVO vo) {
        return Optional.empty();
    }


    @Override
    public Optional<Long> deleteAllPointData() {
        return Optional.empty();
    }

    @Override
    public Optional<Long> deleteOrphanedPointValues() {
        return Optional.empty();
    }

    @Override
    public long dateRangeCount(DataPointVO vo, long from, long to) {
        return 0;
    }

    @Override
    public Optional<Long> getInceptionDate(DataPointVO vo) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getStartTime(Collection<? extends DataPointVO> vos) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getEndTime(Collection<? extends DataPointVO> vos) {
        return Optional.empty();
    }

    @Override
    public Optional<StartAndEndTime> getStartAndEndTime(Collection<? extends DataPointVO> vos) {
        return Optional.empty();
    }

    @Override
    public List<Long> getFiledataIds(DataPointVO vo) {
        return null;
    }

    @Override
    public Optional<Long> deletePointValue(DataPointVO vo, long ts) {
        return Optional.empty();
    }

    @Override
    public void getPointValuesPerPoint(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback) {

    }

    @Override
    public void getPointValuesCombined(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback) {

    }

    @Override
    public void wideBookendQueryPerPoint(Collection<? extends DataPointVO> vos, long from, long to, @Nullable Integer limit, WideCallback<? super IdPointValueTime> callback) {

    }

    @Override
    public void wideBookendQueryCombined(Collection<? extends DataPointVO> vos, long from, long to, @Nullable Integer limit, WideCallback<? super IdPointValueTime> callback) {

    }
}
