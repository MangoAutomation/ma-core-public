/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.statistics.StartsAndRuntime;

/**
 * @author Jared Wiltshire
 */
public interface StartsAndRuntimeAggregate extends AggregateValue {
    List<StartsAndRuntime> getData();

    default Map<Object, StartsAndRuntime> getStartsAndRuntime() {
        return getData().stream()
                .collect(Collectors.toUnmodifiableMap(
                        StartsAndRuntime::getValue,
                        Function.identity()));
    }
}
