/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Terry Packer
 *
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends Exception{

	/**
	 * @param name
	 */
	public ResourceNotFoundException(String name) {
		super("Unable to find resource : " + name); //TODO Internationalize and Strip off pre-web path
	}

	private static final long serialVersionUID = 1L;

}
