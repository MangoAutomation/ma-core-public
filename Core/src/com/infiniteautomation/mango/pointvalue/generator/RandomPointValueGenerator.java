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

public class RandomPointValueGenerator implements PointValueGenerator {

    private final Random random = new Random();
    private final long startTime;
    private final long endTime;
    private final long interval;
    private final double minimum;
    private final double maximum;

    public RandomPointValueGenerator(long startTime, long endTime, long interval, double minimum, double maximum) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.minimum =  minimum;
        this.maximum = maximum;
    }

    @Override
    public Stream<BatchPointValue> apply(DataPointVO point) {
        return Stream.generate(new Supplier<BatchPointValue>() {
            long timestamp = startTime;

            @Override
            public BatchPointValue get() {
                double value = random.nextDouble() * (maximum - minimum) + minimum;
                var pointValueTime = new PointValueTime(value, timestamp);
                timestamp += interval;
                return new BatchPointValueImpl(point, pointValueTime);
            }
        }).takeWhile(v -> v.getPointValue().getTime() < endTime);
    }

}
