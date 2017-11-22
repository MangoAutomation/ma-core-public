/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.io.IOException;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * Callback 
 * @author Terry Packer
 */
public interface BookendQueryCallback<T extends PointValueTime> extends PVTQueryCallback<T> {

    /**
     * Called with the value before the query period (can be null)
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @param index of current row
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query start time for charting
     * @throws IOException to abort query
     */
    default void firstValue(T value, int index, boolean bookend) throws IOException {
        row(value, index);
    }
    
    /**
     * Called with the value before the query period (can be null)
     * 
     * If an exception is thrown the query should be aborted
     * 
     * @param value
     * @param index of current row
     * @throws IOException to abort query
     */
    default void lastValue(T value, int index) throws IOException {
        row(value, index);
    }
    
    
}
