/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.monitor;

/**
 * @author Jared Wiltshire
 */
public interface PollableMonitor<T> extends ValueMonitor<T> {
    public T poll(long timestamp);
}
