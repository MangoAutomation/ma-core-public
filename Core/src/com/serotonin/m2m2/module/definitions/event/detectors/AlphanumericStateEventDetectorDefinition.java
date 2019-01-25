/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AlphanumericStateEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class AlphanumericStateEventDetectorDefinition extends PointEventDetectorDefinition<AlphanumericStateDetectorVO>{

	public static final String TYPE_NAME = "ALPHANUMERIC_STATE";
		
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
	protected AlphanumericStateDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new AlphanumericStateDetectorVO(vo);
	}
	
	@Override
	protected AlphanumericStateDetectorVO createEventDetectorVO(int sourceId) {
	    return new AlphanumericStateDetectorVO(DataPointDao.getInstance().get(sourceId));
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#createModel(com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO)
	 */
	@Override
	public AbstractEventDetectorModel<AlphanumericStateDetectorVO> createModel(
			AbstractEventDetectorVO<AlphanumericStateDetectorVO> vo) {
		return new AlphanumericStateEventDetectorModel((AlphanumericStateDetectorVO) vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getModelClass()
	 */
	@Override
	public Class<?> getModelClass() {
		return AlphanumericStateEventDetectorModel.class;
	}


}
