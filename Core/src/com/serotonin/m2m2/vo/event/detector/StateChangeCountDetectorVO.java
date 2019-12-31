/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.StateChangeCountDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class StateChangeCountDetectorVO extends TimeoutDetectorVO<StateChangeCountDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private int changeCount = 2;
	
	public StateChangeCountDetectorVO(DataPointVO vo) {
		super(vo, new int[] { DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.ALPHANUMERIC });
		this.setDuration(1);
	}

	public int getChangeCount() {
		return changeCount;
	}

	public void setChangeCount(int changeCount) {
		this.changeCount = changeCount;
	}
	
	@Override
	public void validate(ProcessResult response, PermissionService service, PermissionHolder user) {
		super.validate(response, service, user);
		
		if(changeCount <= 1)
			response.addContextualMessage("changeCount", "pointEdit.detectors.invalidChangeCount");

        if(duration <= 0)
            response.addContextualMessage("duration", "validate.greaterThanZero");
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
