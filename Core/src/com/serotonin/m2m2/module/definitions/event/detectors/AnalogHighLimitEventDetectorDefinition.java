/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AnalogHighLimitDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogHighLimitEventDetectorDefinition extends TimeoutDetectorDefinition<AnalogHighLimitDetectorVO> {

	public static final String TYPE_NAME = "HIGH_LIMIT";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.highLimit";
	}

	@Override
	protected AnalogHighLimitDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new AnalogHighLimitDetectorVO(vo);
	}

	@Override
	protected AnalogHighLimitDetectorVO createEventDetectorVO(int sourceId) {
        return new AnalogHighLimitDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	
	   @Override
	    public void validate(ProcessResult response, AnalogHighLimitDetectorVO vo) {
	        super.validate(response, vo);
	        
	        if(vo.isUseResetLimit()) {
	            if(vo.isNotHigher() && vo.getResetLimit() <= vo.getLimit()) {
	                response.addContextualMessage("resetLimit", "validate.greaterThan", vo.getLimit());
	            } else if(!vo.isNotHigher() && vo.getResetLimit() >= vo.getLimit()) {
	                response.addContextualMessage("resetLimit", "validate.lessThan", vo.getLimit());
	            }
	        }
	    }

}
