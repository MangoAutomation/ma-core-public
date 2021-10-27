/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogChangeEventDetectorDefinition extends TimeoutDetectorDefinition<AnalogChangeDetectorVO>{

	public static final String TYPE_NAME = "ANALOG_CHANGE";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.analogChange";
	}

	@Override
	protected AnalogChangeDetectorVO createEventDetectorVO(DataPointVO dp) {
		return new AnalogChangeDetectorVO(dp);
	}

	@Override
	protected AnalogChangeDetectorVO createEventDetectorVO(int sourceId) {
        return new AnalogChangeDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	
    @Override
    public void validate(ProcessResult response, AnalogChangeDetectorVO vo) {
        super.validate(response, vo);
        if(!vo.isCheckIncrease() && !vo.isCheckDecrease()) {
            response.addContextualMessage("checkIncrease", "validate.atLeast1");
            response.addContextualMessage("checkDecrease", "validate.atLeast1");
        }
    }

}
