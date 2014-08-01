/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class RestValidationMessage extends RestMessage{

	private String contextKey;
	
	public RestValidationMessage(){
		super(HttpStatus.NOT_ACCEPTABLE, new TranslatableMessage("common.default"));
		this.contextKey = "";
	}
	/**
	 * @param status
	 * @param message
	 */
	public RestValidationMessage(String contextKey, TranslatableMessage message) {
		super(HttpStatus.NOT_ACCEPTABLE, message);
		this.contextKey = contextKey;
	}

	public String getContextKey(){
		return this.contextKey;
	}
	/**
	 * @param contextKey2
	 */
	public void setContextKey(String contextKey) {
		this.contextKey = contextKey;
		
	}
}
