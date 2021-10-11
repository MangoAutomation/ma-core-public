/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class PerPointIterator implements Iterator<IdPointValueTime> {
    private final PointValueDao dao;
    private final int chunkSize;
    private final TimeOrder sortOrder;
    private final Long startTime;
    private final Long endTime;

    private final Iterator<? extends DataPointVO> pointIterator;
    private Iterator<? extends IdPointValueTime> valueIterator = Collections.emptyIterator();

    /**
     * @param dao       point value DAO
     * @param vos       data points
     * @param startTime start time (epoch ms), inclusive
     * @param endTime   end time (epoch ms), exclusive
     * @param chunkSize number of samples to load from the DAO at once
     * @param sortOrder forward or reverse
     */
    public PerPointIterator(PointValueDao dao, Collection<? extends DataPointVO> vos, @Nullable Long startTime, @Nullable Long endTime,
                            int chunkSize, TimeOrder sortOrder) {
        this.dao = dao;
        this.startTime = startTime;
        this.endTime = endTime;
        this.chunkSize = chunkSize;
        this.sortOrder = sortOrder;
        this.pointIterator = vos.iterator();
    }

    @Override
    public boolean hasNext() {
        return currentValueIterator().hasNext();
    }

    @Override
    public IdPointValueTime next() {
        return currentValueIterator().next();
    }

    private Iterator<? extends IdPointValueTime> currentValueIterator() {
        while (!valueIterator.hasNext() && pointIterator.hasNext()) {
            DataPointVO vo = pointIterator.next();
            this.valueIterator = new PointValueIterator(dao, vo, startTime, endTime, chunkSize, sortOrder);
        }
        return valueIterator;
    }
}
