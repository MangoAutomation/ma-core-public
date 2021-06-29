/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.List;

import com.serotonin.m2m2.rt.script.AbstractPointWrapper;
import com.serotonin.m2m2.rt.script.DataPointWrapper;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Matthew Lohbihler
 */
public interface IDataPointValueSource {

    /**
     * Update the latest value
     * @param newValue
     */
    void updatePointValue(PointValueTime newValue);

    /**
     * Update the latest value optionally asynchronously
     * @param newValue
     * @param async
     */
    void updatePointValue(PointValueTime newValue, boolean async);

    /**
     * Set a point value 
     * @param newValue
     * @param source
     */
    void setPointValue(PointValueTime newValue, SetPointSource source);

    /**
     * Get limit number of latest values 
     * @param limit
     * @return
     */
    List<PointValueTime> getLatestPointValues(int limit);

    
    /**
     * Get the latest/current value
     * @return
     */
    PointValueTime getPointValue();

    /**
     * Get the value before time (can return null)
     * @param time
     * @return
     */
    PointValueTime getPointValueBefore(long time);

    /**
     * Get the value after time (can return null)
     * @param time
     * @return
     */
    PointValueTime getPointValueAfter(long time);

    /**
     * Get all values >= since returned in time order
     * @param since
     * @return
     */
    List<PointValueTime> getPointValues(long since);

    /**
     * Get values >= from and < to returned in time order
     * @param from
     * @param to
     * @return
     */
    List<PointValueTime> getPointValuesBetween(long from, long to);

    /**
     * Get a point value exactly at time (can return null)
     * @param time
     * @return
     */
    PointValueTime getPointValueAt(long time);
    
    /**
     * Get the data type for this source
     * @return
     */
    int getDataTypeId();
    
    DataPointWrapper getDataPointWrapper(AbstractPointWrapper wrapper);
    DataPointVO getVO();
}
