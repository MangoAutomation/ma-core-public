/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;

/**
 * 
 * @author Terry Packer
 */
public class PublisherEventTypeModel extends EventTypeModel{

    private int publisherId;
    private int publisherEventTypeId;
    
    public PublisherEventTypeModel(){ }
    
    public PublisherEventTypeModel(PublisherEventType type){
    	this.publisherId = type.getPublisherId();
    	this.publisherEventTypeId = type.getPublisherEventTypeId();
    }

	public int getPublisherId() {
		return publisherId;
	}

	public void setPublisherId(int publisherId) {
		this.publisherId = publisherId;
	}

	public int getPublisherEventTypeId() {
		return publisherEventTypeId;
	}

	public void setPublisherEventTypeId(int publisherEventTypeId) {
		this.publisherEventTypeId = publisherEventTypeId;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getTypeName()
	 */
	@Override
	public String getTypeName() {
		return EventType.EventTypeNames.PUBLISHER;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#isRateLimited()
	 */
	@Override
	public boolean isRateLimited() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getDuplicateHandling()
	 */
	@Override
	public int getDuplicateHandling() {
		return EventType.DuplicateHandling.IGNORE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getEventTypeInstance()
	 */
	@Override
	public EventType getEventTypeInstance() {
		return new PublisherEventType(publisherId, publisherEventTypeId);
	}
    
    
}
