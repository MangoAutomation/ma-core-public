/*
 * Copyright (C) 2017 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.vo.exception;

import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * @author Jared Wiltshire
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
