/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.BatchPointValueImpl;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.vo.DataPointVO;

public class ConstantPointValueGenerator implements PointValueGenerator {

    private final Instant startTime;
    private final Instant endTime;
    private final Duration period;
    private final DataValue value;

    public ConstantPointValueGenerator(long startTime, long period, DataValue value) {
        this(Instant.ofEpochMilli(startTime), null, Duration.ofMillis(period), value);
    }

    public ConstantPointValueGenerator(long startTime, long endTime, long period, DataValue value) {
        this(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), Duration.ofMillis(period), value);
    }

    public ConstantPointValueGenerator(Instant startTime, Instant endTime, Duration period, DataValue value) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
        this.value = value;
    }

    @Override
    public Stream<BatchPointValue> apply(DataPointVO point) {
        var stream = createSupplier(point).stream();
        if (endTime != null) {
            long endTimeMs = endTime.toEpochMilli();
            stream = stream.takeWhile(v -> v.getPointValue().getTime() < endTimeMs);
        }
        return stream;
    }

    @Override
    public BatchPointValueSupplier createSupplier(DataPointVO point) {
        return new ConstantPointValueSupplier(point);
    }

    private class ConstantPointValueSupplier implements BatchPointValueSupplier {
        final DataPointVO point;

        Instant timestamp = ConstantPointValueGenerator.this.startTime;

        public ConstantPointValueSupplier(DataPointVO point) {
            this.point = point;
        }

        @Override
        public BatchPointValue get() {
            PointValueTime pointValueTime = new PointValueTime(value, timestamp.toEpochMilli());
            this.timestamp = timestamp.plus(period);
            return new BatchPointValueImpl(point, pointValueTime);
        }

        @Override
        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}
