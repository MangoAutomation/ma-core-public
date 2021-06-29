/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.NoUpdateDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class NoUpdateEventDetectorDefinition extends TimeoutDetectorDefinition<NoUpdateDetectorVO>{

	public static final String TYPE_NAME = "NO_UPDATE";
		
	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.noUpdate";
	}

	@Override
	protected NoUpdateDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new NoUpdateDetectorVO(vo);
	}

	@Override
	protected NoUpdateDetectorVO createEventDetectorVO(int sourceId) {
        return new NoUpdateDetectorVO(DataPointDao.getInstance().get(sourceId));
	}
	
	   @Override
	    public void validate(ProcessResult response, NoUpdateDetectorVO vo, PermissionHolder user) {
	        super.validate(response, vo, user);
	        if(vo.getDuration() <= 0)
	            response.addContextualMessage("duration", "validate.greaterThanZero");
	    }

}
