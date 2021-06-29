/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.monitor;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public interface ValueMonitor<T> {

    public String getId();
    public TranslatableMessage getName();
    public void setValue(T value);
    public T getValue();

    /**
     * @return true if value should be sent to the store (periodically or when checking for upgrades)
     */
    public boolean isUploadToStore();

}
