/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.model.RestValidationResponseModel;

/**
 * Controller used to capture errors and 
 * relay the useful information to the user
 * 
 * @author Terry Packer
 *
 */
@ControllerAdvice
public class ExceptionHandlingController {

	private static Logger LOG = Logger.getLogger(ExceptionHandlingController.class);
	
	public ExceptionHandlingController(){
	}
	
	@ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handle(HttpMessageNotReadableException e) {
        LOG.warn("Returning HTTP 400 Bad Request", e);
        //TODO Manage bad JSON formatting here by putting a message into the headers or something
        throw e;
    }
	
	
	/**
	 * Useful when validation fails
	 * @param e
	 * @param request
	 * @return
	 */
    @ExceptionHandler({ RestValidationFailedException.class })
    protected ResponseEntity<RestValidationResponseModel> handleInvalidRequest(RestValidationFailedException e) {
		
		ResponseEntity<RestValidationResponseModel> response = new ResponseEntity<RestValidationResponseModel>(e.getModel(),
				e.getResult().addMessagesToHeaders(new HttpHeaders()),
				e.getResult().getHighestStatus());
		
		return response;
    }
	

	
	
}
