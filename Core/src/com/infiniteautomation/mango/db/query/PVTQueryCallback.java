/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * Base interface for point value time query callbacks
 *
 * @author Terry Packer
 */
public interface PVTQueryCallback<T extends PointValueTime> {

    /**
     * Values within the query
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value
     * @param index
     * @throws QueryCancelledException to abort query
     */
    void row(T value, int index) throws QueryCancelledException;
}
