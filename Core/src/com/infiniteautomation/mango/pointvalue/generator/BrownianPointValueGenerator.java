/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class BrownianPointValueGenerator implements PointValueGenerator {

    private final Random random = new Random();
    private final long startTime;
    private final long endTime;
    private final long interval;
    private final double minimum;
    private final double maximum;
    private final double maxChange;

    public BrownianPointValueGenerator(long startTime, long endTime, long interval, double minimum, double maximum, double maxChange) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.minimum = minimum;
        this.maximum = maximum;
        this.maxChange =  maxChange;
    }

    @Override
    public Stream<BatchPointValue> apply(DataPointVO point) {
        return Stream.generate(new Supplier<BatchPointValue>() {
            long timestamp = startTime;
            double value = (maximum - minimum) / 2 + minimum;

            @Override
            public BatchPointValue get() {
                value += (random.nextDouble() * 2 - 1) * maxChange;
                value = Math.min(Math.max(value, minimum), maximum);

                PointValueTime pointValueTime = new PointValueTime(value, timestamp);
                timestamp += interval;
                return new BatchPointValueImpl(point, pointValueTime);
            }
        }).takeWhile(v -> v.getPointValue().getTime() < endTime);
    }

}
