/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * 
 * @author Terry Packer
 */
public class AnalogChangeEventDetectorModel extends TimeoutDetectorModel<AnalogChangeDetectorVO>{

	public AnalogChangeEventDetectorModel(AnalogChangeDetectorVO data) {
		super(data);
	}

	public AnalogChangeEventDetectorModel() {
		super(new AnalogChangeDetectorVO());
	}

	public double getLimit() {
		return this.data.getLimit();
	}

	public void setLimit(double limit) {
		this.data.setLimit(limit);
	}
	
	public boolean isCheckIncrease() {
	    return this.data.isCheckIncrease();
	}
	
	public void setCheckIncrease(boolean checkIncrease) {
	    this.data.setCheckIncrease(checkIncrease);
	}
	
	public boolean isCheckDecrease() {
        return this.data.isCheckDecrease();
    }
    
    public void setCheckDecrease(boolean checkDecrease) {
        this.data.setCheckDecrease(checkDecrease);
    }
    
    public String getUpdateEvent() {
        return PublisherVO.PUBLISH_TYPE_CODES.getCode(this.data.getUpdateEvent());
    }
    
    public void setUpdateEvent(String updateEventCode) {
        this.data.setUpdateEvent(PublisherVO.PUBLISH_TYPE_CODES.getId(updateEventCode));
    }
}
