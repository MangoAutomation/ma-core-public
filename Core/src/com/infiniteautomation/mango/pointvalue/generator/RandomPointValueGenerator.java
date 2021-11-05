/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.util.Random;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class RandomPointValueGenerator implements PointValueGenerator {

    private final Random random = new Random();
    private final long startTime;
    private final Long endTime;
    private final long interval;
    private final double minimum;
    private final double maximum;

    public RandomPointValueGenerator(long startTime, Long endTime, long interval, double minimum, double maximum) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.minimum =  minimum;
        this.maximum = maximum;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new RandomPointValueSupplier(point);
    }

    @Override
    public Stream<BatchPointValue> apply(DataPointVO point) {
        var stream = createSupplier(point).stream();
        if (endTime != null) {
            stream = stream.takeWhile(v -> v.getPointValue().getTime() < endTime);
        }
        return stream;
    }

    private class RandomPointValueSupplier implements BatchPointValueSupplier {
        final DataPointVO point;
        long timestamp = RandomPointValueGenerator.this.startTime;

        public RandomPointValueSupplier(DataPointVO point) {
            this.point = point;
        }

        @Override
        public BatchPointValue get() {
            double value = random.nextDouble() * (maximum - minimum) + minimum;
            var pointValueTime = new PointValueTime(value, timestamp);
            timestamp += interval;
            return new BatchPointValueImpl(point, pointValueTime);
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
