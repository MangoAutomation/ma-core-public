/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorDefinition extends PointEventDetectorDefinition<RateOfChangeDetectorVO>{

    public static final String TYPE_NAME = "RATE_OF_CHANGE";
    
    @Override
    protected RateOfChangeDetectorVO createEventDetectorVO(DataPointVO dp) {
        return new RateOfChangeDetectorVO(dp);
    }

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.rateOfChange";
    }

}
