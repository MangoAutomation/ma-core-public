/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;

/**
 * 
 * @author Terry Packer
 */
public class SystemEventTypeModel extends EventTypeModel{

    private String systemEventType;
    private int refId1;
    private int duplicateHandling = EventType.DuplicateHandling.ALLOW;

    public SystemEventTypeModel() { }
    
    public SystemEventTypeModel(SystemEventType type){
    	this.systemEventType = type.getSystemEventType();
    	this.refId1 = type.getReferenceId1();
    	this.duplicateHandling = type.getDuplicateHandling();
    }

	public String getSystemEventType() {
		return systemEventType;
	}

	public void setSystemEventType(String systemEventType) {
		this.systemEventType = systemEventType;
	}

	public int getRefId1() {
		return refId1;
	}

	public void setRefId1(int refId1) {
		this.refId1 = refId1;
	}

	public int getDuplicateHandling() {
		return duplicateHandling;
	}

	public void setDuplicateHandling(int duplicateHandling) {
		this.duplicateHandling = duplicateHandling;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getTypeName()
	 */
	@Override
	public String getTypeName() {
		return EventType.EventTypeNames.SYSTEM;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#isRateLimited()
	 */
	@Override
	public boolean isRateLimited() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getEventTypeInstance()
	 */
	@Override
	public EventType getEventTypeInstance() {
		return new SystemEventType(systemEventType, refId1, duplicateHandling);
	}
    
    
}
