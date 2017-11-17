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
     * Called with the value before the query period (can be null)
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query start time for charting
     * @throws IOException to abort query
     */
    void preQuery(T value, boolean bookend) throws IOException;
    
    /**
     * Values within the query
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @param index
     * @throws IOException to abort query
     */
    void row(T value, int index) throws IOException;
    
    /**
     * Called with the value before the query period (can be null)
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query end time for charting
     * @throws IOException to abort query
     */
    void postQuery(T value, boolean bookend) throws IOException;
}