/*
 * Copyright (C) 2017 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.vo.exception;

import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * Thrown when validation fails. Typically from {@link com.serotonin.m2m2.vo.Validatable#ensureValid()}.
 * 
 * This exception is caught and converted to a {@link com.infiniteautomation.mango.rest.v2.exception.ValidationFailedRestException} for REST.
 * 
 * @author Jared Wiltshire
 * @see com.serotonin.m2m2.web.mvc.spring.exception.MangoSpringExceptionHandler
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ProcessResult validationResult;
    
    public ValidationException(ProcessResult validationResult) {
        super("Validation failed");
        this.validationResult = validationResult;
    }

    public ProcessResult getValidationResult() {
        return validationResult;
    }
}
