/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.serotonin.m2m2.web.mvc.rest.v1.model.RestErrorModel;

/**
 * 
 * Class to handle REST Specific Errors and present the user with a Model
 * 
 * 
 * @author Terry Packer
 *
 */
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

	
    @ExceptionHandler({ 
    	NoSupportingModelException.class,
    	ModelNotFoundException.class,
    	Exception.class,
    	RuntimeException.class
    	})
    protected ResponseEntity<Object> handleMangoError(Exception e, WebRequest request) {
    	
    	RestErrorModel error = new RestErrorModel(e);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Messages", "error");
        headers.set("Errors", e.getMessage());
        headers.setContentType(MediaType.APPLICATION_JSON);
    	return handleExceptionInternal(e, error, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
