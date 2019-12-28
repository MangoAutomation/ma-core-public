/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AlphanumericRegexStateDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class AlphanumericRegexStateEventDetectorDefinition extends PointEventDetectorDefinition<AlphanumericRegexStateDetectorVO>{

	public static final String TYPE_NAME = "ALPHANUMERIC_REGEX_STATE";
		

	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.regexState";
	}

	@Override
	protected AlphanumericRegexStateDetectorVO createEventDetectorVO(DataPointVO dp) {
		return new AlphanumericRegexStateDetectorVO(dp);
	}
	
	@Override
	protected AlphanumericRegexStateDetectorVO createEventDetectorVO(int sourceId) {
	    return new AlphanumericRegexStateDetectorVO(DataPointDao.getInstance().get(sourceId, true));
	}
	
}
