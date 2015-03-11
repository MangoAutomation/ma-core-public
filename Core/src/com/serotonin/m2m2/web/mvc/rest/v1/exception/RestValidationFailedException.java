/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.exception;

import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.RestValidationResponseModel;

/**
 * @author Terry Packer
 *
 */
public class RestValidationFailedException extends RestProcessResultException{

	
	private RestValidationResponseModel model;
	
	/**
	 * @param data
	 * @param validationMessages
	 */
	public RestValidationFailedException(Object data, RestProcessResult<?> result) {
		super(result);
		this.model = new RestValidationResponseModel(data, result.getValidationMessages());
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public RestValidationResponseModel getModel(){
		return this.model;
	}
	
}
