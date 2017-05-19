/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public abstract class TimeoutDetectorModel<T extends TimeoutDetectorVO<T>> extends AbstractPointEventDetectorModel<T>{

	public TimeoutDetectorModel(T data) {
		super(data);
	}

	public int getDuration(){
		return this.data.getDuration();
	}
	
	public void setDuration(int duration){
		this.data.setDuration(duration);
	}

	public String getDurationType(){
		return Common.TIME_PERIOD_CODES.getCode(this.data.getDurationType());
	}

	public void setDurationType(String type){
		this.data.setDurationType(Common.TIME_PERIOD_CODES.getId(type));
	}
	
	
}
