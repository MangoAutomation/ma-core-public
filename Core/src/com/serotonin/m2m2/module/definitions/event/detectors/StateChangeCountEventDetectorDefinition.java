/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.StateChangeCountDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class StateChangeCountEventDetectorDefinition extends TimeoutDetectorDefinition<StateChangeCountDetectorVO>{

    public static final String TYPE_NAME = "STATE_CHANGE_COUNT";

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.changeCount";
    }

    @Override
    protected  StateChangeCountDetectorVO createEventDetectorVO(DataPointVO vo) {
        return new StateChangeCountDetectorVO(vo);
    }

    @Override
    protected StateChangeCountDetectorVO createEventDetectorVO(int sourceId) {
        return new StateChangeCountDetectorVO(DataPointDao.getInstance().get(sourceId));
    }

    @Override
    public void validate(ProcessResult response, StateChangeCountDetectorVO vo) {
        super.validate(response, vo);

        if(vo.getChangeCount() <= 1)
            response.addContextualMessage("changeCount", "pointEdit.detectors.invalidChangeCount");
    }

}
