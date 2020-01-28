/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;

public class MultistateStateDetectorRT extends StateDetectorRT<MultistateStateDetectorVO> {

    public MultistateStateDetectorRT(MultistateStateDetectorVO vo) {
        super(vo);
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyText = vo.getDataPoint().getTextRenderer()
                .getText(vo.getState(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();

        if (durationDescription == null)
            return new TranslatableMessage("event.detector.state", name, prettyText);
        return new TranslatableMessage("event.detector.periodState", name, prettyText, durationDescription);
    }

    @Override
    protected boolean stateDetected(PointValueTime newValue) {
        int newMultistate = newValue.getIntegerValue();
        return newMultistate == vo.getState();
    }

    @Override
    public String getThreadNameImpl() {
        return "Multistate State Detector " + this.vo.getXid();
    }

}
