/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AlphanumericStateEventDetectorDefinition extends TimeoutDetectorDefinition<AlphanumericStateDetectorVO>{

	public static final String TYPE_NAME = "ALPHANUMERIC_STATE";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.state";
	}

	@Override
	protected AlphanumericStateDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new AlphanumericStateDetectorVO(vo);
	}
	
	@Override
	protected AlphanumericStateDetectorVO createEventDetectorVO(int sourceId) {
	    return new AlphanumericStateDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	

    @Override
    public void validate(ProcessResult response, AlphanumericStateDetectorVO vo) {
        super.validate(response, vo);
        
        if(vo.getState() == null)
            response.addContextualMessage("state", "emport.error.missingValue", "state");
    }
}
