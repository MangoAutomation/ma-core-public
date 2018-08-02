/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.annotations.ApiModel;

/**
 * @author Terry Packer
 *
 */
@ApiModel(value="Rest Validation Model", description="Holds validation information when a validation fails")
@JsonPropertyOrder({"data", "messages"})
public class RestValidationResponseModel {

	@JsonProperty
	private Object data;
	
	@JsonProperty
	private Map<String,String> messages;

	public RestValidationResponseModel(){
		this.messages = new HashMap<String,String>();
	}
	/**
	 * @param body
	 * @param validationMessages2
	 */
	public RestValidationResponseModel(Object data,
			Map<String, String> messages) {
		this.data = data;
		this.messages = messages;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public Map<String, String> getMessages() {
		return messages;
	}
	public void setMessages(Map<String, String> messages) {
		this.messages = messages;
	}

	
	
	
	
}
