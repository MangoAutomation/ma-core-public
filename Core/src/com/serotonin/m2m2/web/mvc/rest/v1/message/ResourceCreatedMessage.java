/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import java.net.URI;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Message for when a resource is created
 * 
 * @author Terry Packer
 *
 */
public class ResourceCreatedMessage extends RestMessage{

	private URI location;
	
	/**
	 * @param status
	 * @param message
	 */
	public ResourceCreatedMessage(HttpStatus status, TranslatableMessage message, URI location) {
		super(status, message);
		this.location = location;
	}
	
	public URI getLocation(){
		return this.location;
	}
}
