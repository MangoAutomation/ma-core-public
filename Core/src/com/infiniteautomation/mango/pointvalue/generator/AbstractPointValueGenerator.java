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
import com.serotonin.m2m2.vo.DataPointVO;

public abstract class AbstractPointValueGenerator implements PointValueGenerator {

    protected final Instant startTime;
    protected final Instant endTime;
    protected final Duration period;

    public AbstractPointValueGenerator(Instant startTime, Instant endTime, Duration period) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
    }

    @Override
    public Stream<BatchPointValue> apply(DataPointVO point) {
        var stream = createSupplier(point).stream();
        if (endTime != null) {
            long endTimeMs = endTime.toEpochMilli();
            stream = stream.takeWhile(v -> v.getValue().getTime() < endTimeMs);
        }
        return stream;
    }

    public abstract class AbstractPointValueSupplier implements BatchPointValueSupplier {
        protected final DataPointVO point;
        protected Instant timestamp = AbstractPointValueGenerator.this.startTime;

        public AbstractPointValueSupplier(DataPointVO point) {
            this.point = point;
        }

        protected abstract PointValueTime nextPointValue();

        @Override
        public BatchPointValue get() {
            PointValueTime pointValueTime = nextPointValue();
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
