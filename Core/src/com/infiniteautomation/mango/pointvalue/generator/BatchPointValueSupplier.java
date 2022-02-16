/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue.generator;

import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

public interface BatchPointValueSupplier extends Supplier<BatchPointValue<PointValueTime>> {

    Instant getTimestamp();
    void setTimestamp(Instant timestamp);

    default Stream<BatchPointValue<PointValueTime>> stream() {
        return Stream.generate(this);
    }
}
