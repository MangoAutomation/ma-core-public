/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.Arrays;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;

public class MultistateStateDetectorRT extends StateDetectorRT<MultistateStateDetectorVO> {

    public MultistateStateDetectorRT(MultistateStateDetectorVO vo) {
        super(vo);
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String stateText = vo.getStateText();
        TranslatableMessage durationDescription = getDurationDescription();
        boolean inverted = vo.isInverted();
        boolean multipleStates = vo.getStates() != null;

        if (durationDescription == null) {
            if (multipleStates) {
                return new TranslatableMessage(inverted ? "event.detector.notStateIn" : "event.detector.stateIn", name, stateText);
            }
            return new TranslatableMessage(inverted ? "event.detector.notState" : "event.detector.state", name, stateText);
        }

        if (multipleStates) {
            return new TranslatableMessage(inverted ? "event.detector.notPeriodStateIn" : "event.detector.periodStateIn",
                    name, stateText, durationDescription);
        }
        return new TranslatableMessage(inverted ? "event.detector.notPeriodState" : "event.detector.periodState", name,
                stateText, durationDescription);
    }

    @Override
    protected boolean stateDetected(PointValueTime newValue) {
        int value = newValue.getIntegerValue();
        boolean match;

        int[] states = vo.getStates();
        if (states != null) {
            match = Arrays.stream(states).anyMatch(s -> s == value);
        } else {
            match = vo.getState() == value;
        }

        return vo.isInverted() != match;
    }

    @Override
    public String getThreadNameImpl() {
        return "Multistate State Detector " + this.vo.getXid();
    }

}
