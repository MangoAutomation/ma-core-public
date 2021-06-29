/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.NoChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class NoChangeEventDetectorDefinition extends TimeoutDetectorDefinition<NoChangeDetectorVO>{

    public static final String TYPE_NAME = "NO_CHANGE";

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.noChange";
    }

    @Override
    protected NoChangeDetectorVO createEventDetectorVO(DataPointVO vo) {
        return new NoChangeDetectorVO(vo);
    }

    @Override
    protected NoChangeDetectorVO createEventDetectorVO(int sourceId) {
        return new NoChangeDetectorVO(DataPointDao.getInstance().get(sourceId));
    }

}
