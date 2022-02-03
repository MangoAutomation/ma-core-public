/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.stats;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Matthew Lohbihler
 */
public interface IValueTime extends ITime {
    DataValue getValue();

    default boolean isBookend() {
        return false;
    }
    default boolean isFromCache() {
        return false;
    }
}
