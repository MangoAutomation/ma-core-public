/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class BrownianPointValueGenerator extends AbstractPointValueGenerator {

    private final Random random = new Random();
    private final double minimum;
    private final double maximum;
    private final double maxChange;

    public BrownianPointValueGenerator(long startTime, long period) {
        this(Instant.ofEpochMilli(startTime), null, Duration.ofMillis(period));
    }

    public BrownianPointValueGenerator(long startTime, long endTime, long period) {
        this(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), Duration.ofMillis(period));
    }

    public BrownianPointValueGenerator(Instant startTime, Instant endTime, Duration period) {
        this(startTime, endTime, period, 0D, 100D, 0.1D);
    }

    public BrownianPointValueGenerator(Instant startTime, Instant endTime, Duration period, double minimum, double maximum, double maxChange) {
        super(startTime, endTime, period);
        this.minimum = minimum;
        this.maximum = maximum;
        this.maxChange = maxChange;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new BrownianPointValueSupplier(point);
    }

    private class BrownianPointValueSupplier extends AbstractPointValueSupplier {

        private double value;

        public BrownianPointValueSupplier(DataPointVO point) {
            super(point);
            this.value = (maximum - minimum) / 2 + minimum;
        }

        @Override
        protected PointValueTime nextPointValue() {
            var pv = new PointValueTime(value, timestamp.toEpochMilli());
            this.value += (random.nextDouble() * 2 - 1) * maxChange;
            this.value = Math.min(Math.max(value, minimum), maximum);
            return pv;
        }
    }
}
