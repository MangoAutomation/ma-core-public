/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;

/**
 * @author Matthew Lohbihler
 */
public class AlphanumericStateDetectorRT extends StateDetectorRT<AlphanumericStateDetectorVO> {

    public AlphanumericStateDetectorRT(AlphanumericStateDetectorVO vo) {
        super(vo);
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyText = vo.getDataPoint().getTextRenderer()
                .getText(vo.getState(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();

        if (durationDescription != null)
            return new TranslatableMessage("event.detector.periodState", name, prettyText, durationDescription);
        return new TranslatableMessage("event.detector.state", name, prettyText);
    }

    @Override
    protected boolean stateDetected(PointValueTime newValue) {
        String newAlpha = newValue.getStringValue();
        return StringUtils.equals(newAlpha, vo.getState());
    }

    @Override
    public String getThreadNameImpl() {
        return "AlphanumericState Detector " + this.vo.getXid();
    }

}
