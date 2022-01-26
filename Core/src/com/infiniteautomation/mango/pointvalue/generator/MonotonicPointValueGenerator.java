/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class MonotonicPointValueGenerator extends AbstractPointValueGenerator {

    private final Random random = new Random();
    private final double startValue;
    private final double maxChange;

    public MonotonicPointValueGenerator(long startTime, long period) {
        this(Instant.ofEpochMilli(startTime), null, Duration.ofMillis(period));
    }

    public MonotonicPointValueGenerator(long startTime, long endTime, long period) {
        this(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), Duration.ofMillis(period));
    }

    public MonotonicPointValueGenerator(Instant startTime, Instant endTime, Duration period) {
        this(startTime, endTime, period, 0D, 0.1D);
    }

    public MonotonicPointValueGenerator(Instant startTime, Instant endTime, Duration period, double startValue, double maxChange) {
        super(startTime, endTime, period);
        this.startValue = startValue;
        this.maxChange = maxChange;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new MonotonicPointValueSupplier(point);
    }

    private class MonotonicPointValueSupplier extends AbstractPointValueSupplier {

        private double value;

        public MonotonicPointValueSupplier(DataPointVO point) {
            super(point);
            this.value = startValue;
        }

        @Override
        protected PointValueTime nextPointValue() {
            var pv = new PointValueTime(value, timestamp.toEpochMilli());
            this.value += random.nextDouble() * maxChange;
            return pv;
        }
    }
}
