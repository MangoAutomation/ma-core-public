/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractPointEventDetectorVO extends AbstractEventDetectorVO {

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
        if(dataPoint != null) {
            return new EventTypeVO(new DataPointEventType(dataPoint, this), getDescription(),
                    alarmLevel);
        }else {
            return new EventTypeVO(new DataPointEventType(sourceId, id), getDescription(),
                    alarmLevel);
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        if(dataPoint != null) {
            writer.writeEntry("dataPointXid", dataPoint.getXid());
        }
    }

}
