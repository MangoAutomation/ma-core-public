/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class CombinedIterator implements Iterator<IdPointValueTime> {
    private final PointValueDao dao;
    private final Collection<? extends DataPointVO> vos;
    private final int chunkSize;
    private final TimeOrder sortOrder;
    private final Long startTime;
    private final Long endTime;
    private final Queue<PointValueIterator> iterators;

    private boolean initialized = false;

    /**
     * @param dao       point value DAO
     * @param vos       data points
     * @param startTime start time (epoch ms), inclusive
     * @param endTime   end time (epoch ms), exclusive
     * @param chunkSize number of samples to load from the DAO at once
     * @param sortOrder forward or reverse
     */
    public CombinedIterator(PointValueDao dao, Collection<? extends DataPointVO> vos, @Nullable Long startTime, @Nullable Long endTime,
                            int chunkSize, TimeOrder sortOrder) {
        this.dao = dao;
        this.vos = vos;
        this.startTime = startTime;
        this.endTime = endTime;
        this.chunkSize = chunkSize;
        this.sortOrder = sortOrder;

        Comparator<IdPointValueTime> comparator = sortOrder.getComparator().thenComparingInt(IdPointValueTime::getSeriesId);
        // iterators are polled in order of their heads, have to ensure we don't add an empty iterator, or we will get NPE
        this.iterators = new PriorityQueue<>(vos.size(), (a, b) -> comparator.compare(a.peek(), b.peek()));
    }

    @Override
    public boolean hasNext() {
        if (!initialized) {
            initialize();
        }
        return iterators.peek() != null;
    }

    @Override
    public IdPointValueTime next() {
        if (!initialized) {
            initialize();
        }
        return nextValue(iterators.remove());
    }

    /**
     * Gets the next value from the iterator, and re-inserts the iterator into the priority queue if there are more
     * values available.
     *
     * @param it iterator
     * @return iterator's next value
     */
    private IdPointValueTime nextValue(PointValueIterator it) {
        IdPointValueTime result = it.next();
        // only re-insert iterators with data, our comparator does not support nulls
        if (it.hasNext()) {
            // unfortunately, PriorityQueue does not allow access to it's siftDown method
            // we have to remove (remove method above) and re-add the iterator which is less efficient
            iterators.offer(it);
        }
        return result;
    }

    /**
     * Creates all the iterators and adds them to the priority queue if they have a value
     */
    private void initialize() {
        // add iterators for each point to a priority queue
        for (var vo : vos) {
            PointValueIterator it = new PointValueIterator(dao, vo, startTime, endTime, chunkSize, sortOrder);
            // only insert iterators with data, our comparator does not support nulls
            if (it.hasNext()) {
                iterators.offer(it);
            }
        }
        this.initialized = true;
    }
}
