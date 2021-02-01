/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.db;

import java.io.IOException;

/**
 * 
 * Wide Queries are used in Time Series Queries to modify the results of the pre and post query values.
 * 
 * @author Terry Packer
 */
public interface WideQueryCallback<T> {

    /**
     * Called with the value before the query period, will not be called if there is no value before
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @throws IOException to abort query
     */
    void preQuery(T value);
    
    /**
     * Values within the query
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @throws IOException to abort query
     */
    void row(T value);
    
    /**
     * Called with the value before the query period, will not be called if there is no value after.
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @throws IOException to abort query
     */
    void postQuery(T value);
}