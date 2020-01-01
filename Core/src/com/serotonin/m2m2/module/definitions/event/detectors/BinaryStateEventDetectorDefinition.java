/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.BinaryStateDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class BinaryStateEventDetectorDefinition extends PointEventDetectorDefinition<BinaryStateDetectorVO>{

	public static final String TYPE_NAME = "BINARY_STATE";

	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "pointEdit.detectors.state";
	}

	@Override
	protected BinaryStateDetectorVO createEventDetectorVO(DataPointVO vo) {
		return new BinaryStateDetectorVO(vo);
	}

	@Override
    protected BinaryStateDetectorVO createEventDetectorVO(int sourceId) {
        return new BinaryStateDetectorVO(DataPointDao.getInstance().get(sourceId, true));
    }

    @Override
    public void validate(ProcessResult response, BinaryStateDetectorVO ds, PermissionHolder user) {
        
    }

}
