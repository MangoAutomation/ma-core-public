/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * @author Terry Packer
 * 
 */
public abstract class MangoRestController<T> {


	/**
	 * Create a successful response entity for a PUT
	 * @param location
	 * @param response
	 * @param body
	 * @param status
	 * @return
	 */
	protected ResponseEntity<T> createResponseEntity(URI location,
			ProcessResult response, T body, HttpStatus status) {
		HttpHeaders headers = createHeadersFromProcessResult(response);
		headers.setLocation(location);
		return new ResponseEntity<T>(body, headers, status);
		
		
	}
	
	/**
	 * Create a successful response entity for a Delete
	 * @param location
	 * @param response
	 * @param body
	 * @param status
	 * @return
	 */
	protected ResponseEntity<T> createResponseEntity(
			ProcessResult response, T body, HttpStatus status) {
		HttpHeaders headers = createHeadersFromProcessResult(response);
		return new ResponseEntity<T>(body, headers, status);
		
		
	}
	
	
	
	/**
	 * TODO Flesh this out
	 * 
	 * @param response
	 * @return
	 */
	protected ResponseEntity<T> createResponseEntity(ProcessResult response) {
		if (response.getHasMessages()) {
			return new ResponseEntity<T>(
					createHeadersFromProcessResult(response),
					HttpStatus.NOT_ACCEPTABLE);
		} else {
			return new ResponseEntity<T>(
					createHeadersFromProcessResult(response), HttpStatus.OK);
		}
	}

	/**
	 * TODO Flesh this out
	 * 
	 * @param response
	 * @return
	 */
	protected ResponseEntity<T> createResponseEntity(ProcessResult response, HttpStatus status) {
			return new ResponseEntity<T>(
					createHeadersFromProcessResult(response), status);
	}

	/**
	 * TODO Flesh this out
	 * 
	 * @param response
	 * @return
	 */
	protected ResponseEntity<List<T>> createResponseEntityList(
			ProcessResult response, HttpStatus status) {
		return new ResponseEntity<List<T>>(
				createHeadersFromProcessResult(response), status);
	}

	/**
	 * TODO Flesh this out
	 * 
	 * @param response
	 * @return
	 */
	protected ResponseEntity<List<T>> createResponseEntityList(
			ProcessResult response) {

		if (response.getHasMessages()) {
			return new ResponseEntity<List<T>>(
					createHeadersFromProcessResult(response),
					HttpStatus.NOT_ACCEPTABLE);
		} else {
			return new ResponseEntity<List<T>>(
					createHeadersFromProcessResult(response), HttpStatus.OK);
		}

	}
	

	/**
	 * Create headers, adding errors if necessary
	 * 
	 * @param response
	 * @return
	 */
	private HttpHeaders createHeadersFromProcessResult(ProcessResult response) {
		HttpHeaders headers = new HttpHeaders();
		if (response.getHasMessages()) {
			StringBuilder headerErrors = new StringBuilder();
			for (ProcessMessage message : response.getMessages()) {
				headerErrors.append(message.toString(Common.getTranslations()));
			}
			headers.add("errors", headerErrors.toString());
			return headers;
		} else {
			return headers;
		}
	}

}
