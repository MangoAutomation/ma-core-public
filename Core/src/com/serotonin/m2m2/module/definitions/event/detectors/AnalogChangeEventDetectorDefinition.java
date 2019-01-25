/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AnalogChangeEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class AnalogChangeEventDetectorDefinition extends PointEventDetectorDefinition<AnalogChangeDetectorVO>{

	public static final String TYPE_NAME = "ANALOG_CHANGE";
		
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
		return "pointEdit.detectors.analogChange";
	}

	@Override
	protected AnalogChangeDetectorVO createEventDetectorVO(DataPointVO dp) {
		return new AnalogChangeDetectorVO(dp);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#createModel(com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO)
	 */
	@Override
	public AbstractEventDetectorModel<AnalogChangeDetectorVO> createModel(
			AbstractEventDetectorVO<AnalogChangeDetectorVO> vo) {
		return new AnalogChangeEventDetectorModel((AnalogChangeDetectorVO) vo);
	}

	@Override
	protected AnalogChangeDetectorVO createEventDetectorVO(int sourceId) {
        return new AnalogChangeDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getModelClass()
	 */
	@Override
	public Class<?> getModelClass() {
		return AnalogChangeEventDetectorModel.class;
	}

}
