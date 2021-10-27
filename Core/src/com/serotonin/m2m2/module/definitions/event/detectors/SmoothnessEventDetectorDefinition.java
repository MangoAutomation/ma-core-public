/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.SmoothnessDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class SmoothnessEventDetectorDefinition extends TimeoutDetectorDefinition<SmoothnessDetectorVO>{

    public static final String TYPE_NAME = "SMOOTHNESS";

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.smoothness";
    }

    @Override
    protected SmoothnessDetectorVO createEventDetectorVO(DataPointVO vo) {
        return new SmoothnessDetectorVO(vo);
    }

    @Override
    protected SmoothnessDetectorVO createEventDetectorVO(int sourceId) {
        return new SmoothnessDetectorVO(DataPointDao.getInstance().get(sourceId));
    }

    @Override
    public void validate(ProcessResult response, SmoothnessDetectorVO vo) {
        super.validate(response, vo);

        if(Double.isInfinite(vo.getLimit()) || Double.isNaN(vo.getLimit())) {
            response.addContextualMessage("limit", "validate.invalidValue");
        }

        if(Double.isInfinite(vo.getBoxcar()) || Double.isNaN(vo.getBoxcar())) {
            response.addContextualMessage("boxcar", "validate.invalidValue");
        }else if(vo.getBoxcar() <= 2) {
            response.addContextualMessage("boxcar", "validate.greaterThan", 2);
        }
    }

}
