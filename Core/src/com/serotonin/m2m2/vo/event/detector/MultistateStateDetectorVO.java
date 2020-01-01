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
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class MultistateStateDetectorVO extends TimeoutDetectorVO<MultistateStateDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private int state;
	
	public MultistateStateDetectorVO(DataPointVO vo) {
		super(vo, new int[] { DataTypes.MULTISTATE });
	}
	
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	@Override
	public AbstractEventDetectorRT<MultistateStateDetectorVO> createRuntime() {
		return new MultistateStateDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();

        if (durationDesc == null)
            return new TranslatableMessage("event.detectorVo.state", dataPoint.getTextRenderer().getText(
                    state, TextRenderer.HINT_SPECIFIC));
        return new TranslatableMessage("event.detectorVo.statePeriod", dataPoint.getTextRenderer().getText(
                    state, TextRenderer.HINT_SPECIFIC), durationDesc);
	}

}
