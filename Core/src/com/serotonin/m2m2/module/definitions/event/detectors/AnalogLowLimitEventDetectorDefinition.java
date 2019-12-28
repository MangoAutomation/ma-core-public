/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AnalogLowLimitDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogLowLimitEventDetectorDefinition extends PointEventDetectorDefinition<AnalogLowLimitDetectorVO>{

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
        return new AnalogLowLimitDetectorVO(DataPointDao.getInstance().get(sourceId, true));
	}

}
