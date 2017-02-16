/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.exception;

import org.springframework.http.HttpStatus;

/**
 * 
 * @author Terry Packer
 */
public class GenericRestException extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;

	public GenericRestException(HttpStatus httpStatus, Exception e){
		super(httpStatus, null, e);
	}
	
}
