/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.infiniteautomation.mango.rest.v2.model.RestValidationResult;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Exception to provide validation failure information
 * out of the REST api
 * 
 * @author Terry Packer
 */
public class ValidationFailedRestException extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;
	private final RestValidationResult result;

    public ValidationFailedRestException(ProcessResult validationResult) {
        this(new RestValidationResult(validationResult));
    }
    
	public ValidationFailedRestException(RestValidationResult result) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, MangoRestErrorCode.VALIDATION_FAILED, new TranslatableMessage("validate.validationFailed"));
		this.result = result;
	}

    public RestValidationResult getResult(){
		return result;
	}
}
