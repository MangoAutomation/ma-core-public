/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestMessage;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
@ApiModel(value="AbstractRestModel", description="Base Data Model", subTypes={UserModel.class})
public abstract class AbstractRestModel<T> {
	
	@ApiModelProperty(value = "Messages for validation of data", required = false)
	@JsonProperty("validationMessages")
	private Map<String,String> messages;

	@JsonIgnore
	protected T data;
	
	public AbstractRestModel(T data){
		this.data = data;
		this.messages = new HashMap<String,String>();
	}
	
	public void setMessages(Map<String,String> messages){
		this.messages = messages;
	}
	public Map<String,String> getMessages(){
		return this.messages;
	}
	
	/**
	 * Get the data for the model
	 * @return T
	 */
	public T getData(){
		return data;
	}
	
	/**
	 * 
	 * @param response
	 */
	@JsonIgnore
	public void validate(ProcessResult response){
		return;
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