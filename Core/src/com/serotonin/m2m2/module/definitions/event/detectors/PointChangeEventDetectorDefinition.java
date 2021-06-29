/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.PointChangeDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class PointChangeEventDetectorDefinition extends PointEventDetectorDefinition<PointChangeDetectorVO>{

	public static final String TYPE_NAME = "POINT_CHANGE";

	@Override
	public String getEventDetectorTypeName() {
		return TYPE_NAME;
	}

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

    @Override
    public void validate(ProcessResult response, PointChangeDetectorVO ds, PermissionHolder user) {
    }
}
