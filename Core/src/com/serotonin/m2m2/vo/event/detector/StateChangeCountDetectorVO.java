/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.util.EnumSet;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.StateChangeCountDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class StateChangeCountDetectorVO extends TimeoutDetectorVO<StateChangeCountDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private int changeCount = 2;
	
	public StateChangeCountDetectorVO(DataPointVO vo) {
		super(vo, EnumSet.of(DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.ALPHANUMERIC));
		this.setDuration(1);
	}

	public int getChangeCount() {
		return changeCount;
	}

	public void setChangeCount(int changeCount) {
		this.changeCount = changeCount;
	}

	@Override
	public AbstractEventDetectorRT<StateChangeCountDetectorVO> createRuntime() {
		return new StateChangeCountDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
       return new TranslatableMessage("event.detectorVo.changeCount", changeCount, getDurationDescription());
	}

}
