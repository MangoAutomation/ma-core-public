/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller.rest;

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
	 * TODO Flesh this out
	 * 
	 * @param response
	 * @return
	 */
	protected ResponseEntity<T> createResponseEntity(
			ProcessResult response) {
		HttpHeaders headers = new HttpHeaders();
		StringBuilder headerErrors = new StringBuilder();
		for(ProcessMessage message : response.getMessages()){
			headerErrors.append(message.toString(Common.getTranslations()));
		}
		if(response.getHasMessages()){
			headers.add("errors", headerErrors.toString());
			return new ResponseEntity<T>(headers, HttpStatus.NOT_ACCEPTABLE);
		}else{
			return new ResponseEntity<T>(headers, HttpStatus.OK);
		}
		
	}
	
}
