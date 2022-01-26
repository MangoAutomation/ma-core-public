/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.time.Duration;
import java.time.Instant;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.vo.DataPointVO;

public class ConstantPointValueGenerator extends AbstractPointValueGenerator {

    private final DataValue value;

    public ConstantPointValueGenerator(long startTime, long period, DataValue value) {
        this(Instant.ofEpochMilli(startTime), null, Duration.ofMillis(period), value);
    }

    public ConstantPointValueGenerator(long startTime, long endTime, long period, DataValue value) {
        this(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), Duration.ofMillis(period), value);
    }

    public ConstantPointValueGenerator(Instant startTime, Instant endTime, Duration period, DataValue value) {
        super(startTime, endTime, period);
        this.value = value;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new ConstantPointValueSupplier(point);
    }

    private class ConstantPointValueSupplier extends AbstractPointValueSupplier {

        public ConstantPointValueSupplier(DataPointVO point) {
            super(point);
        }

        @Override
        protected PointValueTime nextPointValue() {
            return new PointValueTime(value, timestamp.toEpochMilli());
        }

    }
}
