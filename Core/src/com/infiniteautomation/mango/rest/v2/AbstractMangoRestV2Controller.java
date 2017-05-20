/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.rest.BaseMangoRestController;

/**
 * Base Rest Controller for V2 of the REST api
 * 
 * @author Terry Packer
 */
public abstract class AbstractMangoRestV2Controller extends BaseMangoRestController {
	
	private static final String LOCATION = "Location";
	private static final String ERRORS = "errors";
	private static final String MESSAGES = "messages";
	private static final String COMMA = ",";

	/**
	 * For created resources
	 * @param body
	 * @param location
	 * @return
	 */
	public <N> ResponseEntity<N> getResourceCreated(N body, String location){
		return getResourceModified(body, location, HttpStatus.CREATED);
	}

	public <N> ResponseEntity<N> getResourceDeleted(N body, String location){
		return getResourceModified(body, location, HttpStatus.OK);
	}

	public <N> ResponseEntity<N> getResourceUpdated(N body, String location){
		return getResourceModified(body, location, HttpStatus.OK);
	}

	
	/**
	 * To modify a resource with a Location header
	 * @param body
	 * @param location
	 * @param status
	 * @return
	 */
	protected <N> ResponseEntity<N>getResourceModified(N body, String location, HttpStatus status){
		HttpHeaders headers = new HttpHeaders();
		headers.add(LOCATION, location);
		if(body == null)
			return new ResponseEntity<N>(headers, status);
		else
			return new ResponseEntity<N>(body, headers, status);
	}
	
	/**
	 * Append some errors as a JSON array into the errors header
	 * @param body
	 * @param status
	 * @param errors
	 * @return
	 */
	public <N> ResponseEntity<N> getErrorResponse(N body, HttpStatus status, String ... errors){
		HttpHeaders headers = new HttpHeaders();
		StringBuilder errorMessages = new StringBuilder();
		errorMessages.append("{[");
		for(int i=0; i<errors.length; i++){
			errorMessages.append(errors[i]);
			if(i < errors.length)
				errorMessages.append(COMMA);
		}
		errorMessages.append("]}");
		headers.add(ERRORS, errors.toString());
		return new ResponseEntity<N>(body, headers, status);
	}
	
	/**
	 * Append some messages as a JSON array into the messages header
	 * @param body
	 * @param status
	 * @param errors
	 * @return
	 */
	public <N> ResponseEntity<N> getMessageResponse(N body, HttpStatus status, String ... errors){
		HttpHeaders headers = new HttpHeaders();
		StringBuilder errorMessages = new StringBuilder();
		errorMessages.append("{[");
		for(int i=0; i<errors.length; i++){
			errorMessages.append(errors[i]);
			if(i < errors.length)
				errorMessages.append(COMMA);
		}
		errorMessages.append("]}");
		headers.add(MESSAGES, errors.toString());
		return new ResponseEntity<N>(body, headers, status);
	}
	
	/**
	 * Append some messages as a JSON array into the messages header
	 * @param body
	 * @param status
	 * @param errors
	 * @return
	 */
	public <N> ResponseEntity<N> getMessageResponse(N body, HttpStatus status, TranslatableMessage ... errors){
		HttpHeaders headers = new HttpHeaders();
		StringBuilder errorMessages = new StringBuilder();
		errorMessages.append("{[");
		for(int i=0; i<errors.length; i++){
			errorMessages.append(errors[i].translate(Common.getTranslations()));
			if(i < errors.length)
				errorMessages.append(COMMA);
		}
		errorMessages.append("]}");
		headers.add(MESSAGES, errors.toString());
		return new ResponseEntity<N>(body, headers, status);
	}
}
