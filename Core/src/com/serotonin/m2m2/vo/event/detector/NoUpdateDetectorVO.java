/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.util.EnumSet;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.NoUpdateDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class NoUpdateDetectorVO extends TimeoutDetectorVO<NoUpdateDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	public NoUpdateDetectorVO(DataPointVO vo) {
		super(vo, EnumSet.allOf(DataTypes.class));
		this.setDuration(1);
	}
	
	@Override
	public AbstractEventDetectorRT<NoUpdateDetectorVO> createRuntime() {
		return new NoUpdateDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
		return new TranslatableMessage("event.detectorVo.noUpdate", getDurationDescription());
	}

}
