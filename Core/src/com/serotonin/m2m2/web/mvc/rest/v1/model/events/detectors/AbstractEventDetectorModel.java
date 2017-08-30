/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractEventDetectorModel<T extends AbstractEventDetectorVO<T>> extends AbstractVoModel<T>{

	/**
	 * @param data
	 */
	public AbstractEventDetectorModel(T data) {
		super(data);
	}
	
	@JsonGetter("detectorType")
	@Override
	public String getModelType(){
		return this.data.getDefinition().getEventDetectorTypeName();
	}
	
	public String getAlias() {
		return this.data.getAlias();
	}

	public void setAlias(String alias) {
		this.data.setAlias(alias);
	}
	public int getSourceId(){
		return this.data.getSourceId();
	}
	public void setSourceId(int id){
		this.data.setSourceId(id);
	}
	
	public boolean isRtnApplicable(){
		return this.data.isRtnApplicable();
	}
	
	public EventTypeModel getEventType(){
		return this.data.getEventType().createEventType().asModel();
	}
	
	public TranslatableMessage getDescription(){
		return this.data.getDescription();
	}
	
	public String getDetectorSourceType(){
		return this.data.getDetectorSourceType();
	}
	
	public List<String> getHandlers() {
	    return EventHandlerDao.instance.getEventHandlerXids(this.data.getEventType());
	}
	
	public void setHandlers(List<String> handlerXids) {
	    this.data.addEventHandlers(handlerXids);;
	}

	/**
	 * @param def
	 */
	@JsonIgnore
	public void setDefinition(EventDetectorDefinition<?> def) {
		this.data.setDefinition(def);
	}

}
