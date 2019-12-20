/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.monitor;

/**
 * @author Jared Wiltshire
 */
public interface PollableMonitor<T> extends ValueMonitor<T> {
    public T poll(long timestamp);
}
