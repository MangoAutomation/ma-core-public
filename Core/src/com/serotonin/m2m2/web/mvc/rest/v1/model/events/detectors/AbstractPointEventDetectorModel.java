/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 *
 * @author Terry Packer
 */
public abstract class AbstractPointEventDetectorModel<T extends AbstractPointEventDetectorVO<T>> extends AbstractEventDetectorModel<T> {

    public AbstractPointEventDetectorModel(T data) {
        super(data);
    }

    public AlarmLevels getAlarmLevel(){
        return this.data.getAlarmLevel();
    }
    public void setAlarmLevel(AlarmLevels level){
        this.data.setAlarmLevel(level);
    }

    public String[] getSupportedDataTypes(){
        int[] supportedTypeInts = this.data.getSupportedDataTypes();
        String[] supportedTypes = new String[supportedTypeInts.length];
        for(int i=0; i<supportedTypeInts.length; i++)
            supportedTypes[i] = DataTypes.CODES.getCode(supportedTypeInts[i]);
        return supportedTypes;
    }
}
