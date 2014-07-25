/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.RealTimeDataPointValue;

/**
 * @author Terry Packer
 *
 */
public class RealTimeModel extends AbstractRestModel<RealTimeDataPointValue>{

	/**
	 * @param data
	 */
	public RealTimeModel(RealTimeDataPointValue data) {
		super(data);
	}

	@JsonGetter(value="deviceName")
	public String getDeviceName(){
		return this.data.getDeviceName();
	}
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.model.AbstractRestModel#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
	@Override
	public void validate(ProcessResult response) {
		//No-op as data can't be set from this model... yet.
	}

}
