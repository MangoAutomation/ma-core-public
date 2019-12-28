/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.StateChangeCountDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class StateChangeCountEventDetectorDefinition extends PointEventDetectorDefinition<StateChangeCountDetectorVO>{

	public static final String TYPE_NAME = "STATE_CHANGE_COUNT";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}
	
	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.changeCount";
	}

	@Override
	protected  StateChangeCountDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new StateChangeCountDetectorVO(vo);
	}

	@Override
	protected StateChangeCountDetectorVO createEventDetectorVO(int sourceId) {
        return new StateChangeCountDetectorVO(DataPointDao.getInstance().get(sourceId, true));

	}
}
