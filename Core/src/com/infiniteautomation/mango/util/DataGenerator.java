/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class DataGenerator implements Function<DataPointVO, Stream<BatchPointValue>> {

    private final Random random = new Random();
    private final long startTime;
    private final long endTime;
    private final long interval;

    public DataGenerator(long startTime, long endTime, long interval) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
    }

    @Override
    public Stream<BatchPointValue> apply(DataPointVO point) {
        AtomicLong timestamp = new AtomicLong(startTime);
        return Stream.generate((Supplier<BatchPointValue>) () -> {
            var value = new PointValueTime(random.nextDouble(), timestamp.getAndAdd(interval));
            return new BatchPointValueImpl(point, value);
        }).takeWhile(v -> v.getPointValue().getTime() < endTime);
    }

    public DataInserter createInserter(PointValueDao pointValueDao, int chunkSize) {
        return new DataInserter(this, chunkSize, pointValueDao);
    }
}
