/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.UpdateDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
public class UpdateEventDetectorDefinition extends PointEventDetectorDefinition<UpdateDetectorVO> {

    public static final String TYPE_NAME = "UPDATE";

    @Override
    public String getEventDetectorTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "pointEdit.detectors.update";
    }

    @Override
    protected UpdateDetectorVO createEventDetectorVO(DataPointVO vo) {
        return new UpdateDetectorVO(vo);
    }

    @Override
    public void validate(ProcessResult response, UpdateDetectorVO ds, PermissionHolder user) {
    }
}
