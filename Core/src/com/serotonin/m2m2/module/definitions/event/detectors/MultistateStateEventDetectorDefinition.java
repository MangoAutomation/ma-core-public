/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class MultistateStateEventDetectorDefinition extends TimeoutDetectorDefinition<MultistateStateDetectorVO>{

    public static final String TYPE_NAME = "MULTISTATE_STATE";

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.state";
    }

    @Override
    protected MultistateStateDetectorVO createEventDetectorVO(DataPointVO vo) {
        return new MultistateStateDetectorVO(vo);
    }

    @Override
    protected MultistateStateDetectorVO createEventDetectorVO(int sourceId) {
        return new MultistateStateDetectorVO(DataPointDao.getInstance().get(sourceId));
    }
}
