/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.PositiveCusumDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class PositiveCusumEventDetectorDefinition extends TimeoutDetectorDefinition<PositiveCusumDetectorVO>{

	public static final String TYPE_NAME = "POSITIVE_CUSUM";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.posCusum";
	}

	@Override
	protected PositiveCusumDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new PositiveCusumDetectorVO(vo);
	}
	
	@Override
	protected PositiveCusumDetectorVO createEventDetectorVO(int sourceId) {
        return new PositiveCusumDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	
    @Override
    public void validate(ProcessResult response, PositiveCusumDetectorVO vo) {
        super.validate(response, vo);
        
        if(Double.isInfinite(vo.getLimit()) || Double.isNaN(vo.getLimit()))
            response.addContextualMessage("limit", "validate.invalidValue");
        if(Double.isInfinite(vo.getWeight()) || Double.isNaN(vo.getWeight()))
            response.addContextualMessage("weight", "validate.invalidValue");
    }	
}
