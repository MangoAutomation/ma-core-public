/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.HighLimitRateOfChangeDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.HighLimitRateOfChangeDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class HighLimitRateOfChangeDetectorDefinition extends PointEventDetectorDefinition<HighLimitRateOfChangeDetectorVO>{

    public static final String TYPE_NAME = "HIGH_LIMIT_RATE_OF_CHANGE";
    
    @Override
    protected HighLimitRateOfChangeDetectorVO createEventDetectorVO(DataPointVO dp) {
        return new HighLimitRateOfChangeDetectorVO(dp);
    }

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.highLimitRateOfChange";
    }

    @Override
    public AbstractEventDetectorModel<HighLimitRateOfChangeDetectorVO> createModel(
            AbstractEventDetectorVO<HighLimitRateOfChangeDetectorVO> vo) {
        return new HighLimitRateOfChangeDetectorModel((HighLimitRateOfChangeDetectorVO)vo);
    }

    @Override
    public Class<?> getModelClass() {
        return HighLimitRateOfChangeDetectorModel.class;
    }

}
