/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.NegativeCusumDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class NegativeCusumEventDetectorDefinition extends TimeoutDetectorDefinition<NegativeCusumDetectorVO>{

	public static final String TYPE_NAME = "NEGATIVE_CUSUM";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.negCusum";
	}

	@Override
	protected NegativeCusumDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new NegativeCusumDetectorVO(vo);
	}

	@Override
	protected NegativeCusumDetectorVO createEventDetectorVO(int sourceId) {
        return new NegativeCusumDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	
	   @Override
	    public void validate(ProcessResult response, NegativeCusumDetectorVO vo) {
	        super.validate(response, vo);
	        
	        if(Double.isInfinite(vo.getLimit()) || Double.isNaN(vo.getLimit()))
	            response.addContextualMessage("limit", "validate.invalidValue");
	        if(Double.isInfinite(vo.getWeight()) || Double.isNaN(vo.getWeight()))
	            response.addContextualMessage("weight", "validate.invalidValue");
	    }
}
