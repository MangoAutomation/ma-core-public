/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.BinaryStateDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class BinaryStateEventDetectorDefinition extends TimeoutDetectorDefinition<BinaryStateDetectorVO>{

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
        return new BinaryStateDetectorVO(DataPointDao.getInstance().get(sourceId));
    }
}
