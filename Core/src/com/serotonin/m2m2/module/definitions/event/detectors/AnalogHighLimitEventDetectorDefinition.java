/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogHighLimitDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AnalogHighLimitEventDetectorModel;

/**
 * @author Terry Packer
 *
 */
public class AnalogHighLimitEventDetectorDefinition extends PointEventDetectorDefinition<AnalogHighLimitDetectorVO>{

	public static final String TYPE_NAME = "HIGH_LIMIT";
		
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
		return "pointEdit.detectors.highLimit";
	}

	@Override
	protected AnalogHighLimitDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new AnalogHighLimitDetectorVO(vo);
	}

	@Override
	protected AnalogHighLimitDetectorVO createEventDetectorVO(int sourceId) {
        return new AnalogHighLimitDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#createModel(com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO)
	 */
	@Override
	public AbstractEventDetectorModel<AnalogHighLimitDetectorVO> createModel(
			AbstractEventDetectorVO<AnalogHighLimitDetectorVO> vo) {
		return new AnalogHighLimitEventDetectorModel((AnalogHighLimitDetectorVO)vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventDetectorDefinition#getModelClass()
	 */
	@Override
	public Class<?> getModelClass() {
		return AnalogHighLimitEventDetectorModel.class;
	}

}
