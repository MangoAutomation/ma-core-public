/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonView;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JsonViews;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestMessage;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractVoModel<T extends AbstractVO<T>> extends AbstractRestModel<AbstractVO<T>>{
	
	
	/**
	 * @param data
	 */
	public AbstractVoModel(AbstractVO<T> data) {
		super(data);
		this.messages = new HashMap<String,String>();

	}

	@JsonGetter("xid")
	public String getXid(){
		return this.data.getXid();
	}
	@JsonSetter("xid")
	public void setXid(String xid){
		this.data.setXid(xid);
	}
	
	@JsonGetter("name")
	public String getName(){
		return this.data.getName();
	}
	@JsonSetter("name")
	public void setName(String name){
		this.data.setName(name);
	}
	
	@ApiModelProperty(value = "Messages for validation of data", required = false)
	@JsonProperty("validationMessages")
	@JsonView(JsonViews.Validation.class) //Only show in validation views
	private Map<String,String> messages;
	
	public void setMessages(Map<String,String> messages){
		this.messages = messages;
	}
	public Map<String,String> getMessages(){
		return this.messages;
	}	
	

	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel#validate(com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult)
	 */
	@Override
	public void validate(RestProcessResult<?> result) throws RestValidationFailedException {
		ProcessResult validation = new ProcessResult();
		this.data.validate(validation);
		
		if(validation.getHasMessages()){
			result.addValidationMessages(validation);
			throw new RestValidationFailedException(this, result);
		}
	}

	/**
	 * Add messages from a validation
	 * 
	 * @param validation
	 */
	@JsonIgnore
	public RestMessage addValidationMessages(ProcessResult validation) {
		
		if(this.messages == null)
			this.messages = new HashMap<String,String>();
		
		for(ProcessMessage message : validation.getMessages()){
			this.messages.put(message.getContextKey(), message.getContextualMessage().translate(Common.getTranslations()));
		}
		
		return new RestMessage(HttpStatus.NOT_ACCEPTABLE, new TranslatableMessage("common.default", "Validation error"));
	}
}
