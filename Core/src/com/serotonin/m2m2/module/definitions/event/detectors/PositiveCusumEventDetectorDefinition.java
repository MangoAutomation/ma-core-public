/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.PositiveCusumDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class PositiveCusumEventDetectorDefinition extends PointEventDetectorDefinition<PositiveCusumDetectorVO>{

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
        return new PositiveCusumDetectorVO(DataPointDao.getInstance().get(sourceId, true));
	}
}
