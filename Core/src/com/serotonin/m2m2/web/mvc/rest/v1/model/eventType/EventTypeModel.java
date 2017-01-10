/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * 
 * @author Terry Packer
 */
public abstract class EventTypeModel{
	
	/**
	 * Type of Event Model
	 * @return
	 */
	abstract public String getTypeName();
	
	abstract public boolean isRateLimited();
	
	abstract public int getDuplicateHandling();
	
	/**
	 * Create an instance of the Event Type
	 * @return
	 */
	@JsonIgnore
	abstract public EventType getEventTypeInstance();
	
}
