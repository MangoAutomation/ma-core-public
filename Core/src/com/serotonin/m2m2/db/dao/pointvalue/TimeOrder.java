/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.util.Comparator;

import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;

/**
 * @author Jared Wiltshire
 */
public enum TimeOrder {
    /**
     * Ascending time order, i.e. oldest values first
     */
    ASCENDING(Comparator.comparingLong(IdPointValueTime::getTime)),

    /**
     * Descending time order, i.e. newest values first
     */
    DESCENDING(Comparator.comparingLong(IdPointValueTime::getTime).reversed());

    private final Comparator<IdPointValueTime> comparator;

    TimeOrder(Comparator<IdPointValueTime> comparator) {
        this.comparator = comparator;
    }

    public Comparator<IdPointValueTime> getComparator() {
        return comparator;
    }
}
