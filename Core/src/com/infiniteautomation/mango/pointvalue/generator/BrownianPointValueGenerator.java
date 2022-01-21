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

public class BrownianPointValueGenerator implements PointValueGenerator {

    private final Random random = new Random();
    private final long startTime;
    private final Long endTime;
    private final long period;
    private final double minimum;
    private final double maximum;
    private final double maxChange;

    public BrownianPointValueGenerator(long startTime, long period) {
        this(startTime, null, period, 0D, 100D, 0.1D);
    }

    public BrownianPointValueGenerator(long startTime, long endTime, long period) {
        this(startTime, endTime, period, 0D, 100D, 0.1D);
    }

    public BrownianPointValueGenerator(long startTime, Long endTime, long period, double minimum, double maximum, double maxChange) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
        this.minimum = minimum;
        this.maximum = maximum;
        this.maxChange =  maxChange;
    }

    @Override
    public Stream<BatchPointValue> apply(DataPointVO point) {
        var stream = createSupplier(point).stream();
        if (endTime != null) {
            stream = stream.takeWhile(v -> v.getPointValue().getTime() < endTime);
        }
        return stream;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new BrownianPointValueSupplier(point);
    }

    private class BrownianPointValueSupplier implements BatchPointValueSupplier {
        final DataPointVO point;

        long timestamp = BrownianPointValueGenerator.this.startTime;
        double value;

        public BrownianPointValueSupplier(DataPointVO point) {
            this.point = point;
            this.value = (maximum - minimum) / 2 + minimum;
        }

        @Override
        public BatchPointValue get() {
            this.value += (random.nextDouble() * 2 - 1) * maxChange;
            this.value = Math.min(Math.max(value, minimum), maximum);

            PointValueTime pointValueTime = new PointValueTime(value, timestamp);
            this.timestamp += period;
            return new BatchPointValueImpl(point, pointValueTime);
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
