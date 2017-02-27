/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Exception for when a Resource cannot be found
 * 
 * @author Terry Packer
 */
public class NotFoundRestException extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;
	
	public NotFoundRestException(){
		super(HttpStatus.NOT_FOUND, new TranslatableMessage("rest.notFound"));
	}
}
