/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.MultistateBitDetectorVO;

/**
 * @author Jared Wiltshire
 */
public class MultistateBitDetectorRT extends StateDetectorRT<MultistateBitDetectorVO> {

    public MultistateBitDetectorRT(MultistateBitDetectorVO vo) {
        super(vo);
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String stateText = vo.getBitMaskText();
        TranslatableMessage durationDescription = getDurationDescription();
        boolean inverted = vo.isInverted();

        if (durationDescription == null) {
            return new TranslatableMessage(inverted ? "event.detector.notMatchesMask" : "event.detector.matchesMask",
                    name, stateText);
        }

        return new TranslatableMessage(inverted ? "event.detector.notMatchesMaskPeriod" : "event.detector.matchesMaskPeriod",
                name, stateText, durationDescription);
    }

    @Override
    protected boolean stateDetected(PointValueTime newValue) {
        int value = newValue.getIntegerValue();
        boolean match = (value & vo.getBitmask()) != 0;
        return vo.isInverted() != match;
    }

    @Override
    public String getThreadNameImpl() {
        return "Multistate Bit Detector " + this.vo.getXid();
    }

}
