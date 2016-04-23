/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.StateChangeCountDetectorRT;

/**
 * @author Terry Packer
 *
 */
public class StateChangeCountDetectorVO extends TimeoutDetectorVO<StateChangeCountDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private int changeCount;
	
	public StateChangeCountDetectorVO() {
		super(new int[] { DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.ALPHANUMERIC });
	}

	public int getChangeCount() {
		return changeCount;
	}

	public void setChangeCount(int changeCount) {
		this.changeCount = changeCount;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#createRuntime()
	 */
	@Override
	public AbstractEventDetectorRT<StateChangeCountDetectorVO> createRuntime() {
		return new StateChangeCountDetectorRT(this);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#getConfigurationDescription()
	 */
	@Override
	protected TranslatableMessage getConfigurationDescription() {
       return new TranslatableMessage("event.detectorVo.changeCount", changeCount, getDurationDescription());
	}

}
