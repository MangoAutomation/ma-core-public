/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.query;

import java.util.Optional;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

public class SinglePVTCallback<T extends PointValueTime> implements PVTQueryCallback<T> {
    private T singleValue;

    @Override
    public void row(T value) {
        if (this.singleValue != null) throw new IllegalStateException("Already set");
        this.singleValue = value;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(singleValue);
    }
}
