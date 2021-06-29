/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public abstract class TimeoutDetectorDefinition<T extends TimeoutDetectorVO<T>> extends PointEventDetectorDefinition<T> {
    
    @Override
    public void validate(ProcessResult response, T vo, PermissionHolder user) {
        super.validate(response, vo, user);
        if (!Common.TIME_PERIOD_CODES.isValidId(vo.getDurationType()))
            response.addContextualMessage("durationType", "validate.invalidValue");
        if (vo.getDuration() < 0)
            response.addContextualMessage("duration", "validate.cannotBeNegative");
    }
}
