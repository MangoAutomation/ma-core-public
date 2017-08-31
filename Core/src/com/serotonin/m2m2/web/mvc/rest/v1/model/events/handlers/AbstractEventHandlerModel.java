/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractEventHandlerModel <T extends AbstractEventHandlerVO<T>> extends AbstractVoModel<T>{

	/**
	 * @param data
	 */
	public AbstractEventHandlerModel(T data) {
		super(data);
	}
	
	@JsonGetter("handlerType")
	@Override
	public String getModelType(){
		return this.data.getDefinition().getEventHandlerTypeName();
	}
	
	public String getAlias(){
		return this.data.getAlias();
	}
	
	public void setAlias(String alias){
		this.data.setAlias(alias);
	}
	
    public boolean isDisabled() {
        return this.data.isDisabled();
    }

    public void setDisabled(boolean disabled) {
        this.data.setDisabled(disabled);
    }

	@JsonIgnore
	public void setDefinition(EventHandlerDefinition<?> def) {
		this.data.setDefinition(def);
	}
	
	public List<EventTypeModel> getEventTypes() {
	    List<EventType> events = EventHandlerDao.instance.getEventTypesForHandler(this.data.getId());
	    List<EventTypeModel> models = new ArrayList<>(events.size());
	    for(EventType e : events)
	        models.add(e.asModel());
	    return models;
	}
	
	public void setEventTypes(List<EventTypeModel> eventTypes) {
	    for(EventTypeModel etm : eventTypes) {
	        this.data.addEventType(etm.getEventTypeInstance());
	    }
	}
	
}
