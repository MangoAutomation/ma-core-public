/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.time.Duration;
import java.time.Instant;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class LinearPointValueGenerator extends AbstractPointValueGenerator {

    private final double startValue;
    private final double increment;

    public LinearPointValueGenerator(long startTime, long period) {
        this(Instant.ofEpochMilli(startTime), null, Duration.ofMillis(period));
    }

    public LinearPointValueGenerator(long startTime, long endTime, long period) {
        this(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), Duration.ofMillis(period));
    }

    public LinearPointValueGenerator(Instant startTime, Instant endTime, Duration period) {
        this(startTime, endTime, period, 0D, 0.1D);
    }

    public LinearPointValueGenerator(Instant startTime, Instant endTime, Duration period, double startValue, double increment) {
        super(startTime, endTime, period);
        this.startValue = startValue;
        this.increment = increment;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new LinearPointValueSupplier(point);
    }

    private class LinearPointValueSupplier extends AbstractPointValueSupplier {

        private double value;

        public LinearPointValueSupplier(DataPointVO point) {
            super(point);
            this.value = startValue;
        }

        @Override
        protected PointValueTime nextPointValue() {
            var pv = new PointValueTime(value, timestamp.toEpochMilli());
            this.value += increment;
            return pv;
        }
    }
}
