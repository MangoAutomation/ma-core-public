/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.List;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.script.AbstractPointWrapper;
import com.serotonin.m2m2.rt.script.DataPointWrapper;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Matthew Lohbihler
 */
public interface IDataPointValueSource {

    /**
     * Update the latest value
     */
    void updatePointValue(PointValueTime newValue);

    /**
     * Update the latest value optionally asynchronously
     */
    void updatePointValue(PointValueTime newValue, boolean async);

    /**
     * Set a point value
     */
    void setPointValue(PointValueTime newValue, SetPointSource source);

    /**
     * Get limit number of latest values
     */
    List<PointValueTime> getLatestPointValues(int limit);

    
    /**
     * Get the latest/current value
     */
    PointValueTime getPointValue();

    /**
     * Get the value before time (can return null)
     */
    PointValueTime getPointValueBefore(long time);

    /**
     * Get the value after time (can return null)
     */
    PointValueTime getPointValueAfter(long time);

    /**
     * Get all values greater than or equal to since returned in time order
     */
    List<PointValueTime> getPointValues(long since);

    /**
     * Get values greater than or equal to from and less than to returned in time order
     */
    List<PointValueTime> getPointValuesBetween(long from, long to);

    /**
     * Get a point value exactly at time (can return null)
     */
    PointValueTime getPointValueAt(long time);
    
    /**
     * Get the data type for this source
     */
    DataTypes getDataType();
    
    DataPointWrapper getDataPointWrapper(AbstractPointWrapper wrapper);
    DataPointVO getVO();
}
