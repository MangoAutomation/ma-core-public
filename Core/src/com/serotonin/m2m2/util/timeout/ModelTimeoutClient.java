/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util.timeout;

public interface ModelTimeoutClient<T> {
    void scheduleTimeout(T model, long fireTime);
}
