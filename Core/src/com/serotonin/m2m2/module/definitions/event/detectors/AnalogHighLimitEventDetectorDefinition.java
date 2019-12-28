/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AnalogHighLimitDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogHighLimitEventDetectorDefinition extends PointEventDetectorDefinition<AnalogHighLimitDetectorVO>{

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
        return new AnalogHighLimitDetectorVO(DataPointDao.getInstance().get(sourceId, true));
	}

}
