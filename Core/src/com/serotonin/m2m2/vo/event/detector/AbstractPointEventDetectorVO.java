/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import org.apache.commons.lang3.ArrayUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractPointEventDetectorVO<T extends AbstractPointEventDetectorVO<T>> extends AbstractEventDetectorVO<T> {

    private static final long serialVersionUID = 1L;

    public static final String XID_PREFIX = "PED_";

    //Extra Fields
    protected final DataPointVO dataPoint;
    private final int[] supportedDataTypes;

    public AbstractPointEventDetectorVO(DataPointVO dataPoint, int[] supportedDataTypes){
        this.sourceId = dataPoint == null ? Common.NEW_ID : dataPoint.getId();
        this.dataPoint = dataPoint;
        this.supportedDataTypes = supportedDataTypes;
    }

    public DataPointVO getDataPoint() {
        return dataPoint;
    }

    /**
     * What data types are supported
     * @param dataType
     * @return
     */
    public boolean supports(int dataType) {
        return ArrayUtils.contains(supportedDataTypes, dataType);
    }

    public int[] getSupportedDataTypes(){
        return supportedDataTypes;
    }

    @Override
    public boolean isRtnApplicable() {
        return true;
    }

    @Override
    public EventTypeVO getEventType() {
        return new EventTypeVO(new DataPointEventType(sourceId, id), getDescription(),
                alarmLevel);
    }
}
