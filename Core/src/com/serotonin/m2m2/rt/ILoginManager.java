/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import com.infiniteautomation.mango.monitor.AtomicIntegerMonitor;

/**
 * @author Terry Packer
 *
 */
public interface ILoginManager {
    /**
     * Get the monitor for session counts
     * @return
     */
    public AtomicIntegerMonitor getSessionCountMonitor();
}
