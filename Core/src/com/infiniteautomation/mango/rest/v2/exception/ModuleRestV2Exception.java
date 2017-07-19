/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * 
 * @author Terry Packer
 */
public class ModuleRestV2Exception extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;
	private int statusCode;
	
	/**
	 * @param httpCode
	 * @param e
	 */
	public ModuleRestV2Exception(HttpStatus httpCode, int statusCode, Exception e) {
		super(httpCode, e);
		if(statusCode > 999)
			throw new ShouldNeverHappenException("Custom module status codes must be < 1000");
	}
	
	public ModuleRestV2Exception(HttpStatus status, int statusCode, TranslatableMessage message){
		super(status, message);
		if(statusCode > 999)
			throw new ShouldNeverHappenException("Custom module status codes must be < 1000");
	}

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.rest.v2.exception.AbstractRestV2Exception#getMangoStatusCode()
	 */
	@Override
	public int getMangoStatusCode() {
		return statusCode;
	}
	
}
