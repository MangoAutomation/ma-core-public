/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import java.util.Arrays;
import java.util.stream.Collectors;

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
	/**
	 * takes precedence over state if not null
	 */
	@JsonProperty
	private int[] states;
	@JsonProperty
	private boolean inverted;

	public MultistateStateDetectorVO(DataPointVO vo) {
		super(vo, new int[] { DataTypes.MULTISTATE });
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	public int[] getStates() {
		return states;
	}

	public void setStates(int[] states) {
		this.states = states;
	}

	@Override
	public AbstractEventDetectorRT<MultistateStateDetectorVO> createRuntime() {
		return new MultistateStateDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();

        String stateText = getStateText();
        if (durationDesc == null) {
			return new TranslatableMessage(inverted ? "event.detectorVo.notState" : "event.detectorVo.state",
					stateText);
		}
        return new TranslatableMessage(inverted ? "event.detectorVo.notStatePeriod" : "event.detectorVo.statePeriod",
				stateText, durationDesc);
	}

	public String getStateText() {
		if (states != null) {
			return Arrays.stream(states)
					.mapToObj(s -> dataPoint.getTextRenderer().getText(s, TextRenderer.HINT_SPECIFIC))
					.collect(Collectors.joining(", "));
		}
		return dataPoint.getTextRenderer().getText(state, TextRenderer.HINT_SPECIFIC);
	}

}
