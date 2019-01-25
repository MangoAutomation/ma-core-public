/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.PositiveCusumDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.PositiveCusumEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class PositiveCusumEventDetectorDefinition extends PointEventDetectorDefinition<PositiveCusumDetectorVO>{

	public static final String TYPE_NAME = "POSITIVE_CUSUM";
		
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
		return "pointEdit.detectors.posCusum";
	}

	@Override
	protected PositiveCusumDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new PositiveCusumDetectorVO(vo);
	}
	
	@Override
	protected PositiveCusumDetectorVO createEventDetectorVO(int sourceId) {
        return new PositiveCusumDetectorVO(DataPointDao.getInstance().get(sourceId));
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#createModel(com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO)
	 */
	@Override
	public AbstractEventDetectorModel<PositiveCusumDetectorVO> createModel(
			AbstractEventDetectorVO<PositiveCusumDetectorVO> vo) {
		return new PositiveCusumEventDetectorModel((PositiveCusumDetectorVO)vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getModelClass()
	 */
	@Override
	public Class<?> getModelClass() {
		return PositiveCusumEventDetectorModel.class;
	}
}
