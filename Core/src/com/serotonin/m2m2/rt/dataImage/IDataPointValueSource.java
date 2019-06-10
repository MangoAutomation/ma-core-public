/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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
    List<PointValueTime> getLatestPointValues(int limit);

    void updatePointValue(PointValueTime newValue);

    void updatePointValue(PointValueTime newValue, boolean async);

    /**
     * Set the value, optionally supply a source for the annotation
     * @param newValue
     * @param source
     */
    void setPointValue(PointValueTime newValue, SetPointSource source);

    /**
     * Get the current value
     * @return
     */
    PointValueTime getPointValue();

    /**
     * Get the nearest point value before time
     * @param time
     * @return
     */
    PointValueTime getPointValueBefore(long time);

    /**
     * Get the point value at or just after this time
     * @param time
     * @return
     */
    PointValueTime getPointValueAfter(long time);

    /**
     * Get values >= since
     * @param since
     * @return
     */
    List<PointValueTime> getPointValues(long since);

    /**
     * Get point values >= from < to
     * @param from
     * @param to
     * @return
     */
    List<PointValueTime> getPointValuesBetween(long from, long to);

    /**
     * Get the point value exactly at this time or return null if there isn't one
     * @param time
     * @return
     */
    PointValueTime getPointValueAt(long time);
    
    int getDataTypeId();
    
    DataPointWrapper getDataPointWrapper(AbstractPointWrapper wrapper);
    DataPointVO getVO();
}
