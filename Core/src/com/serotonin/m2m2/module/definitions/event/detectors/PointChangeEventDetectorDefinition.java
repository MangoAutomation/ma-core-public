/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.PointChangeDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.PointChangeEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class PointChangeEventDetectorDefinition extends PointEventDetectorDefinition<PointChangeDetectorVO>{

	public static final String TYPE_NAME = "POINT_CHANGE";
		
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
		return "pointEdit.detectors.change";
	}

	@Override
	protected PointChangeDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new PointChangeDetectorVO(vo);
	}
	
	@Override
	protected PointChangeDetectorVO createEventDetectorVO(int sourceId) {
        return new PointChangeDetectorVO(DataPointDao.getInstance().get(sourceId));
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#createModel(com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO)
	 */
	@Override
	public AbstractEventDetectorModel<PointChangeDetectorVO> createModel(
			AbstractEventDetectorVO<PointChangeDetectorVO> vo) {
		return new PointChangeEventDetectorModel((PointChangeDetectorVO) vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getModelClass()
	 */
	@Override
	public Class<?> getModelClass() {
		return PointChangeEventDetectorModel.class;
	}
}
