/*
	Copyright (C) 2013 Infinite Automation. All rights reserved.
	@author Phillip Dunlap
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.regex.Pattern;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.AlphanumericRegexStateDetectorVO;

public class AlphanumericRegexStateDetectorRT extends StateDetectorRT<AlphanumericRegexStateDetectorVO> {

    public AlphanumericRegexStateDetectorRT(AlphanumericRegexStateDetectorVO vo) {
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
        if(newAlpha != null)
            return Pattern.compile(vo.getState()).matcher(newAlpha).find();
        return false;
    }

    @Override
    public String getThreadNameImpl() {
        return "AlphanumericRegex Detector " + this.vo.getXid();
    }

}