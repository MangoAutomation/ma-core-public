/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.infiniteautomation.mango.web.mvc.rest.v2.model.exception.RestExceptionModel;

/**
 * 
 * @author Terry Packer
 */
public abstract class AbstractRestV2Exception extends RuntimeException{

	private static final long serialVersionUID = 1L;

	/**
	 * Get the Status for the Exception
	 * @return
	 */
	abstract public HttpStatus getStatus();
	
	/**
	 * Get the model for the response Body
	 * @return
	 */
	abstract public RestExceptionModel getBodyModel();
	
}
