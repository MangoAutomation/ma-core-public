/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Iterator interface for querying point values for a single data point. Loads and buffers "chunkSize"
 * samples from the DAO at a time.
 */
public class PointValueIterator implements Iterator<IdPointValueTime> {
    private final PointValueDao dao;
    private final DataPointVO vo;
    private final int chunkSize;
    private final TimeOrder sortOrder;

    private Long startTime;
    private Long endTime;
    private final Deque<IdPointValueTime> buffer;
    /**
     * Indicates that no more data is available for the time range
     */
    private boolean exhausted = false;
    /**
     * Number of values left before hitting limit
     */
    private Integer remaining;

    /**
     * @param dao point value DAO
     * @param vo data point
     * @param startTime start time (epoch ms), inclusive
     * @param endTime end time (epoch ms), exclusive
     * @param limit limit number of values returned
     * @param sortOrder forward or reverse
     * @param chunkSize number of samples to load from the DAO at once
     */
    public PointValueIterator(PointValueDao dao, DataPointVO vo, @Nullable Long startTime, @Nullable Long endTime,
                              @Nullable Integer limit, TimeOrder sortOrder, int chunkSize) {
        this.dao = dao;
        this.vo = vo;
        this.startTime = startTime;
        this.endTime = endTime;
        this.remaining = limit;
        this.chunkSize = chunkSize;
        this.sortOrder = sortOrder;
        this.buffer = new ArrayDeque<>(chunkSize);
    }

    @Override
    public boolean hasNext() {
        return peek() != null;
    }

    @Override
    public IdPointValueTime next() {
        fillQueue();
        return buffer.remove();
    }

    /**
     * Loads more data from the DAO if the buffer is empty
     */
    private void fillQueue() {
        if (!exhausted && buffer.isEmpty()) {
            int limit = remaining == null ? chunkSize : Math.min(chunkSize, remaining);
            if (limit == 0) {
                this.exhausted = true;
                return;
            }
            dao.getPointValuesPerPoint(Collections.singleton(vo), startTime, endTime, limit, sortOrder, buffer::offer);
            if (remaining != null) {
                remaining -= buffer.size();
            }

            IdPointValueTime value = buffer.peekLast();
            if (value == null) {
                this.exhausted = true;
            } else {
                // update start/end time for next query
                if (sortOrder == TimeOrder.ASCENDING) {
                    // start time is inclusive, have to add one, so we don't get the same sample twice
                    startTime = value.getTime() + 1;
                } else {
                    endTime = value.getTime();
                }
            }
        }
    }

    /**
     * Peek at the next value without removing it
     * @return next value
     */
    public @Nullable IdPointValueTime peek() {
        fillQueue();
        return buffer.peek();
    }

    public Spliterator<IdPointValueTime> toSpliterator() {
        return Spliterators.spliteratorUnknownSize(this,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.SORTED);
    }

    public Stream<IdPointValueTime> toStream() {
        return StreamSupport.stream(toSpliterator(), false);
    }
}
