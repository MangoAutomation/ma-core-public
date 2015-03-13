/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public class BaseRestMessage {
	
	@JsonProperty
	protected String message; //Translated Message String
	@JsonProperty
	protected RestMessageLevel level; //Message level
	
	public BaseRestMessage(){
		
	}
	
	public BaseRestMessage(String message, RestMessageLevel level){
		this.message = message;
		this.level = level;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public RestMessageLevel getLevel() {
		return level;
	}

	public void setLevel(RestMessageLevel level) {
		this.level = level;
	}
	
}
