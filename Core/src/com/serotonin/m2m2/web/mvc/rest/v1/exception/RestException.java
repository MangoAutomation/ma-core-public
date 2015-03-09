/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.exception;

import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;

/**
 * @author Terry Packer
 *
 */
public class RestException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected RestProcessResult<?> result;
	
	public RestException(RestProcessResult<?> result){
		this.result = result;
	}
	
	public RestProcessResult<?> getResult(){
		return result;
	}
	
}
