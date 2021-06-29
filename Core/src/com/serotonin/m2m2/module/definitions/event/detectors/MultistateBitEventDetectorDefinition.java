/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.MultistateBitDetectorVO;

/**
 * @author Jared Wiltshire
 */
public class MultistateBitEventDetectorDefinition extends TimeoutDetectorDefinition<MultistateBitDetectorVO> {

    public static final String TYPE_NAME = "MULTISTATE_BIT";

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.bit";
    }

    @Override
    protected MultistateBitDetectorVO createEventDetectorVO(DataPointVO vo) {
        return new MultistateBitDetectorVO(vo);
    }

    @Override
    protected MultistateBitDetectorVO createEventDetectorVO(int sourceId) {
        return new MultistateBitDetectorVO(DataPointDao.getInstance().get(sourceId));
    }
}
