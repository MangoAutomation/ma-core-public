/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.util.EnumSet;

import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.PointChangeDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class PointChangeDetectorVO extends AbstractPointEventDetectorVO {

    private static final long serialVersionUID = 1L;

    public PointChangeDetectorVO(DataPointVO vo) {
        super(vo, EnumSet.of(
                DataType.BINARY,
                DataType.MULTISTATE,
                DataType.NUMERIC,
                DataType.ALPHANUMERIC));
    }

    @Override
    public AbstractEventDetectorRT<PointChangeDetectorVO> createRuntime() {
        return new PointChangeDetectorRT(this);
    }

    @Override
    protected TranslatableMessage getConfigurationDescription() {
        return new TranslatableMessage("event.detectorVo.change");
    }

    @Override
    public boolean isRtnApplicable() {
        return false;
    }

}
