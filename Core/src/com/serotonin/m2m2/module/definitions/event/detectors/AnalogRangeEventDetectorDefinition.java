/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AnalogRangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogRangeEventDetectorDefinition extends PointEventDetectorDefinition<AnalogRangeDetectorVO>{

	public static final String TYPE_NAME = "RANGE";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.range";
	}

	protected AnalogRangeDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new AnalogRangeDetectorVO(vo);
	}

	@Override
	protected AnalogRangeDetectorVO createEventDetectorVO(int sourceId) {
        return new AnalogRangeDetectorVO(DataPointDao.getInstance().get(sourceId, true));

	}
}
