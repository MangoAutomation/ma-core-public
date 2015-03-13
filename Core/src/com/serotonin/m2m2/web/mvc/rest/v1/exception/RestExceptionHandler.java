/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
 * This is a work-in-progress and will be improved on as the API develops
 * 
 * @author Terry Packer
 *
 */
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ 
    	NoSupportingModelException.class,
    	ModelNotFoundException.class
    	})
    protected ResponseEntity<RestErrorModel> handleMangoError(Exception e, WebRequest request) {
    	return processErrorModel(e, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


	/**
	 * A single place to customize the response body of all Exception types.
	 * This method returns {@code null} by default.
	 * @param ex the exception
	 * @param body the body to use for the response
	 * @param headers the headers to be written to the response
	 * @param status the selected response status
	 * @param request the current request
	 */
    @Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
			request.setAttribute("javax.servlet.error.exception", ex, WebRequest.SCOPE_REQUEST);
		}
    	RestErrorModel error = new RestErrorModel(ex);
        headers.set("Messages", "error");
        headers.set("Errors", ex.getMessage());
		return new ResponseEntity<Object>(error, headers, status);
	}

	

	/**
	 * 
	 * @param e
	 * @param status
	 * @param request
	 * @return
	 */
	private ResponseEntity<RestErrorModel> processErrorModel(Exception e, HttpStatus status, WebRequest request){
    	RestErrorModel error = new RestErrorModel(e);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Messages", "error");
        headers.set("Errors", e.getMessage());
        headers.set("Content-Type", "text");
        return new ResponseEntity<RestErrorModel>(error, headers, status);

	}

}
