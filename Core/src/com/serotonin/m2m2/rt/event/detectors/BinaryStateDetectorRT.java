/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.BinaryStateDetectorVO;

public class BinaryStateDetectorRT extends StateDetectorRT<BinaryStateDetectorVO> {

    public BinaryStateDetectorRT(BinaryStateDetectorVO vo) {
        super(vo);
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyText = vo.getDataPoint().getTextRenderer()
                .getText(vo.isState(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();

        if (durationDescription == null)
            return new TranslatableMessage("event.detector.state", name, prettyText);
        return new TranslatableMessage("event.detector.periodState", name, prettyText, durationDescription);
    }

    @Override
    protected boolean stateDetected(PointValueTime newValue) {
        boolean newBinary = newValue.getBooleanValue();
        return newBinary == vo.isState();
    }

    @Override
    public String getThreadNameImpl() {
        return "BinaryState Detector " + this.vo.getXid();
    }

}
