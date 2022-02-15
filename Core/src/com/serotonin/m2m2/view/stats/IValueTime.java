/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.stats;

/**
 * @author Matthew Lohbihler
 */
public interface IValueTime<T> extends ITime {
    T getValue();

    default boolean isBookend() {
        return false;
    }
    default boolean isFromCache() {
        return false;
    }
}
