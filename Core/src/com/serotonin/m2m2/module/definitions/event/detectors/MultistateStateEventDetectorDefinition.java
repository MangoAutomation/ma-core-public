/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.MultistateStateEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class MultistateStateEventDetectorDefinition extends PointEventDetectorDefinition<MultistateStateDetectorVO>{

	public static final String TYPE_NAME = "MULTISTATE_STATE";
		
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getEventDetectorSubTypeName()
	 */
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getDescriptionKey()
	 */
	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.state";
	}

	@Override
	protected MultistateStateDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new MultistateStateDetectorVO(vo);
	}

	 @Override
	protected MultistateStateDetectorVO createEventDetectorVO(int sourceId) {
	     return new MultistateStateDetectorVO(DataPointDao.getInstance().get(sourceId));
	}

	 /* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#createModel(com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO)
	 */
	@Override
	public AbstractEventDetectorModel<MultistateStateDetectorVO> createModel(
			AbstractEventDetectorVO<MultistateStateDetectorVO> vo) {
		return new MultistateStateEventDetectorModel((MultistateStateDetectorVO)vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getModelClass()
	 */
	@Override
	public Class<?> getModelClass() {
		return MultistateStateEventDetectorModel.class;
	}
}
