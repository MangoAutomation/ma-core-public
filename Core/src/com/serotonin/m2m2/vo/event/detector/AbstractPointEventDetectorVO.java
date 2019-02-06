/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import org.apache.commons.lang3.ArrayUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
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
    protected static final String MISSING_PROP_TRANSLATION_KEY = "emport.error.ped.missingAttr";


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
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#isRtnApplicable()
     */
    @Override
    public boolean isRtnApplicable() {
        return true;
    }

    @Override
    public EventTypeVO getEventType() {
        return new EventTypeVO(new DataPointEventType(sourceId, id), getDescription(),
                alarmLevel);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.AbstractVO#validate(com.serotonin.m2m2.i18n.ProcessResult)
     */
    @Override
    public void validate(ProcessResult response) {
        super.validate(response);


        //We currently don't check to see if the point exists
        // because of SQL constraints

        //Is valid data type
        boolean valid = false;
        for(int type : this.supportedDataTypes){
            if(type == this.dataPoint.getPointLocator().getDataTypeId()){
                valid = true;
                break;
            }
        }
        if(!valid){
            //Add message
            response.addContextualMessage("dataPoint.pointLocator.dataType", "validate.invalidValue");
        }

        //Is valid alarm level
        if (alarmLevel == null)
            response.addContextualMessage("alarmLevel", "validate.invalidValue");

    }
}
