/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

/**
 * Interface to indicate that the service has one or many caches
 *  which can be invalidated.
 *
 * @author Terry Packer
 */
public interface CachingService {

    /**
     * This will invalidate all caches on the service
     */
    void clearCaches();
}
