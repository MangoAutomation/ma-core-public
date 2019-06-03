/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.LowLimitRateOfChangeDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.LowLimitRateOfChangeDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class LowLimitRateOfChangeDetectorDefinition extends PointEventDetectorDefinition<LowLimitRateOfChangeDetectorVO>{

    public static final String TYPE_NAME = "LOW_LIMIT_RATE_OF_CHANGE";
    
    @Override
    protected LowLimitRateOfChangeDetectorVO createEventDetectorVO(DataPointVO dp) {
        return new LowLimitRateOfChangeDetectorVO(dp);
    }

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.lowLimitRateOfChange";
    }

    @Override
    public AbstractEventDetectorModel<LowLimitRateOfChangeDetectorVO> createModel(
            AbstractEventDetectorVO<LowLimitRateOfChangeDetectorVO> vo) {
        return new LowLimitRateOfChangeDetectorModel((LowLimitRateOfChangeDetectorVO)vo);
    }

    @Override
    public Class<?> getModelClass() {
        return LowLimitRateOfChangeDetectorModel.class;
    }
}
