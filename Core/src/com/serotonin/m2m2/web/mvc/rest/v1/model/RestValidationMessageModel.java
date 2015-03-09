/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestValidationMessage;

/**
 * @author Terry Packer
 *
 */
public class RestValidationMessageModel extends AbstractRestModel<RestValidationMessage>{

	public RestValidationMessageModel(){
		super(new RestValidationMessage());
	}
	/**
	 * @param data
	 */
	@JsonIgnore
	public RestValidationMessageModel(RestValidationMessage data) {
		super(data);
	}
	
	@JsonGetter("contextKey")
	public String getContextKey() {
		return this.data.getContextKey();
	}
	@JsonSetter("contextKey")
	public void setContextKey(String contextKey) {
		this.data.setContextKey(contextKey);
	}
	
	@JsonGetter("message")
	public String getMessage() {
		return this.data.getMessage();
	}
	@JsonSetter("message")
	public void setMessage(String message) {
		this.data.setMessage(message);
	}
	
	
}
