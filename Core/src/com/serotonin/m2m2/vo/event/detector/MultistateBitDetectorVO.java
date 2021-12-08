/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.vo.event.detector;

import java.util.EnumSet;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.MultistateBitDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
public class MultistateBitDetectorVO extends TimeoutDetectorVO<MultistateBitDetectorVO> {

	private static final long serialVersionUID = 1L;

	@JsonProperty
	private int bitmask;

	@JsonProperty
	private boolean inverted;

	public MultistateBitDetectorVO(DataPointVO vo) {
		super(vo, EnumSet.of(DataTypes.MULTISTATE));
	}

	public int getBitmask() {
		return bitmask;
	}

	public void setBitmask(int bitmask) {
		this.bitmask = bitmask;
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	@Override
	public AbstractEventDetectorRT<MultistateBitDetectorVO> createRuntime() {
		return new MultistateBitDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();

        String maskText = getBitMaskText();
        if (durationDesc == null) {
			return new TranslatableMessage(inverted ? "event.detectorVo.notMatchesMask" : "event.detectorVo.matchesMask",
					maskText);
		}
        return new TranslatableMessage(inverted ? "event.detectorVo.notMatchesMaskPeriod" : "event.detectorVo.matchesMaskPeriod",
				maskText, durationDesc);
	}

	public String getBitMaskText() {
		return "0x" + Integer.toHexString(bitmask);
	}

}
