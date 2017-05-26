/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.db;

/**
 * 
 * Wide Queries are used in Time Series Queries to return the value prior to and after the desired query period.
 * 
 * @author Terry Packer
 */
public interface WideQueryCallback<T> {
	
	/**
	 * Called once with the value before the query period (can be null)
	 * @param value
	 */
    void preQuery(T value);

    /**
     * Values within the query
     * @param value
     * @param index
     */
    void sample(T value, int index);

    /**
     * Called once with the value after the query period (can be null
     * @param value
     */
    void postQuery(T value);
}