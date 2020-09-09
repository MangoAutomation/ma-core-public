/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import com.serotonin.m2m2.DataTypes;
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
        super(vo, new int[]{
                DataTypes.BINARY,
                DataTypes.MULTISTATE,
                DataTypes.NUMERIC,
                DataTypes.ALPHANUMERIC,
                DataTypes.IMAGE});
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
