/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.MultistateStateDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * @author Terry Packer
 *
 */
public class MultistateStateDetectorVO extends TimeoutDetectorVO<MultistateStateDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private int state;
	
	public MultistateStateDetectorVO() {
		super(new int[] { DataTypes.MULTISTATE });
	}
	
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
//	@Override
//	public void validate(ProcessResult response) {
//		super.validate(response);
//	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#createRuntime()
	 */
	@Override
	public AbstractEventDetectorRT<MultistateStateDetectorVO> createRuntime() {
		return new MultistateStateDetectorRT(this);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#getConfigurationDescription()
	 */
	@Override
	protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage message;
        TranslatableMessage durationDesc = getDurationDescription();

        if (durationDesc == null)
            message = new TranslatableMessage("event.detectorVo.state", dataPoint.getTextRenderer().getText(
                    state, TextRenderer.HINT_SPECIFIC));
        else
            message = new TranslatableMessage("event.detectorVo.statePeriod", dataPoint.getTextRenderer().getText(
                    state, TextRenderer.HINT_SPECIFIC), durationDesc);
        return message;
	}

}
