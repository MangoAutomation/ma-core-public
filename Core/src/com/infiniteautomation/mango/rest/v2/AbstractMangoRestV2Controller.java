/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.serotonin.m2m2.web.mvc.rest.BaseMangoRestController;

/**
 * Base Rest Controller for V2 of the REST api
 * 
 * @author Terry Packer
 */
public abstract class AbstractMangoRestV2Controller extends BaseMangoRestController {

	/**
	 * For created resources
	 * @param body
	 * @param location
	 * @return
	 */
	public static <N> ResponseEntity<N> getResourceCreated(N body, URI location) {
		return getResourceModified(body, location, HttpStatus.CREATED);
	}

	public static <N> ResponseEntity<N> getResourceUpdated(N body, URI location) {
		return getResourceModified(body, location, HttpStatus.OK);
	}

	/**
	 * To modify a resource with a Location header
	 * @param body
	 * @param location
	 * @param status
	 * @return
	 */
	protected static <N> ResponseEntity<N> getResourceModified(N body, URI location, HttpStatus status) {
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(location);
		if (body == null)
			return new ResponseEntity<N>(headers, status);
		else
			return new ResponseEntity<N>(body, headers, status);
	}
}
