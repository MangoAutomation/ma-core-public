/*
 * Copyright (C) 2017 Infinite Automation Systems Inc. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Thrown when validation fails. Typically from {@link com.serotonin.m2m2.vo.Validatable#ensureValid()}.
 *
 * This exception is caught and converted to a {@link com.infiniteautomation.mango.rest.latest.exception.ValidationFailedRestException} for REST.
 *
 * @author Jared Wiltshire
 * @see com.infiniteautomation.mango.rest.latest.MangoSpringExceptionHandler
 */
public class ValidationException extends TranslatableRuntimeException {
    private static final long serialVersionUID = 1L;

    private final ProcessResult validationResult;
    private final Class<?> validatedClass;

    public ValidationException(ProcessResult validationResult) {
        super(new TranslatableMessage("validate.validationFailed"));
        this.validationResult = validationResult;
        this.validatedClass = null;
    }

    public ValidationException(ProcessResult validationResult, Class<?> validatedClass) {
        super(new TranslatableMessage("validate.validationFailed"));
        this.validationResult = validationResult;
        this.validatedClass = validatedClass;
    }

    public ProcessResult getValidationResult() {
        return validationResult;
    }

    public Class<?> getValidatedClass() {
        return validatedClass;
    }
}
