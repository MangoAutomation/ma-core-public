/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.SmoothnessDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.SmoothnessDetectorEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class SmoothnessEventDetectorDefinition extends PointEventDetectorDefinition<SmoothnessDetectorVO>{

	public static final String TYPE_NAME = "SMOOTHNESS";
		
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
		return "pointEdit.detectors.smoothness";
	}

	protected SmoothnessDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new SmoothnessDetectorVO(vo);
	}

	@Override
	protected SmoothnessDetectorVO createEventDetectorVO(int sourceId) {
        return new SmoothnessDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#createModel(com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO)
	 */
	@Override
	public AbstractEventDetectorModel<SmoothnessDetectorVO> createModel(
			AbstractEventDetectorVO<SmoothnessDetectorVO> vo) {
		return new SmoothnessDetectorEventDetectorModel((SmoothnessDetectorVO)vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getModelClass()
	 */
	@Override
	public Class<?> getModelClass() {
		return SmoothnessDetectorEventDetectorModel.class;
	}
}
