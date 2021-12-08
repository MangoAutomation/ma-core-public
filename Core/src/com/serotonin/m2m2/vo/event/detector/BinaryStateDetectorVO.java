/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.util.EnumSet;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.BinaryStateDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class BinaryStateDetectorVO extends TimeoutDetectorVO<BinaryStateDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private boolean state;
	
	public BinaryStateDetectorVO(DataPointVO vo) {
		super(vo, EnumSet.of(DataTypes.BINARY));
	}
	
	public boolean isState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	@Override
	public AbstractEventDetectorRT<BinaryStateDetectorVO> createRuntime() {
		return new BinaryStateDetectorRT(this);
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
