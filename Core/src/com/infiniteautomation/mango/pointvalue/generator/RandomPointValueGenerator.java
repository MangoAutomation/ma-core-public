/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class RandomPointValueGenerator extends AbstractPointValueGenerator {

    private final Random random = new Random();
    private final double minimum;
    private final double maximum;

    public RandomPointValueGenerator(long startTime, long period) {
        this(Instant.ofEpochMilli(startTime), null, Duration.ofMillis(period));
    }

    public RandomPointValueGenerator(long startTime, long endTime, long period) {
        this(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), Duration.ofMillis(period));
    }

    public RandomPointValueGenerator(Instant startTime, Instant endTime, Duration period) {
        this(startTime, endTime, period, 0D, 100D);
    }

    public RandomPointValueGenerator(Instant startTime, Instant endTime, Duration period, double minimum, double maximum) {
        super(startTime, endTime, period);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new RandomPointValueSupplier(point);
    }

    private class RandomPointValueSupplier extends AbstractPointValueSupplier {

        public RandomPointValueSupplier(DataPointVO point) {
            super(point);
        }

        @Override
        protected PointValueTime nextPointValue() {
            double value = random.nextDouble() * (maximum - minimum) + minimum;
            return new PointValueTime(value, timestamp.toEpochMilli());
        }
    }
}
