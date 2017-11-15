/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for when a REST item cannot be found
 * 
 * @author Terry Packer
 */
public class NotFoundRestException extends AbstractRestV2Exception {

	private static final long serialVersionUID = 1L;
	
	public NotFoundRestException() {
		super(HttpStatus.NOT_FOUND);
	}
	
	public NotFoundRestException(Throwable cause) {
        super(HttpStatus.NOT_FOUND, cause);
    }
}
