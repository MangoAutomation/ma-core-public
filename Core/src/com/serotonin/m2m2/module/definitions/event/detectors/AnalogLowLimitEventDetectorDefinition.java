/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AnalogLowLimitDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogLowLimitEventDetectorDefinition extends TimeoutDetectorDefinition<AnalogLowLimitDetectorVO>{

	public static final String TYPE_NAME = "LOW_LIMIT";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.lowLimit";
	}

	@Override
	protected AnalogLowLimitDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new AnalogLowLimitDetectorVO(vo);
	}

	@Override
	protected AnalogLowLimitDetectorVO createEventDetectorVO(int sourceId) {
        return new AnalogLowLimitDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	
    @Override
    public void validate(ProcessResult response, AnalogLowLimitDetectorVO vo) {
        super.validate(response, vo);
        
        if(vo.isUseResetLimit()) {
            if(!vo.isNotLower() && vo.getResetLimit() <= vo.getLimit()) {
                response.addContextualMessage("resetLimit", "validate.greaterThan", vo.getLimit());
            } else if(vo.isNotLower() && vo.getResetLimit() >= vo.getLimit()) {
                response.addContextualMessage("resetLimit", "validate.lessThan", vo.getLimit());
            }
        }
    }	

}
