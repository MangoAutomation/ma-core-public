/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.util.EnumSet;

import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.UpdateDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
public class UpdateDetectorVO extends AbstractPointEventDetectorVO {

    private static final long serialVersionUID = 1L;

    public UpdateDetectorVO(DataPointVO vo) {
        super(vo, EnumSet.allOf(DataType.class));
    }

    @Override
    public AbstractEventDetectorRT<UpdateDetectorVO> createRuntime() {
        return new UpdateDetectorRT(this);
    }

    @Override
    protected TranslatableMessage getConfigurationDescription() {
        return new TranslatableMessage("event.detectorVo.update");
    }

    @Override
    public boolean isRtnApplicable() {
        return false;
    }

}
