/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.SmoothnessDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class SmoothnessEventDetectorDefinition extends PointEventDetectorDefinition<SmoothnessDetectorVO>{

	public static final String TYPE_NAME = "SMOOTHNESS";

	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.smoothness";
	}

	protected SmoothnessDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new SmoothnessDetectorVO(vo);
	}

	@Override
	protected SmoothnessDetectorVO createEventDetectorVO(int sourceId) {
        return new SmoothnessDetectorVO(DataPointDao.getInstance().get(sourceId, true));
	}
}
