/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.stats;

import java.util.Comparator;

/**
 * Interface for NoSQL Time Series Objects
 * 
 * Enforcement to have a time to store against
 * 
 * @author Terry Packer
 *
 */
public interface ITime {

	Comparator<ITime> COMPARATOR = Comparator.comparingLong(ITime::getTime);

	/**
	 * Get the time
     */
    long getTime();
}
