/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * @author Matthew Lohbihler
 */
public class AlphanumericStateDetectorRT extends StateDetectorRT<AlphanumericStateDetectorVO> {
    
	public AlphanumericStateDetectorRT(AlphanumericStateDetectorVO vo) {
        super(vo);
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.njbGetDataPoint().getName();
        String prettyText = vo.njbGetDataPoint().getTextRenderer()
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
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return "AlphanumericState Detector " + this.vo.getXid();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.highPriorityRejectionHandler.rejected(reason);
	}
}
