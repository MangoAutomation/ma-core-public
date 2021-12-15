/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * @author Jared Wiltshire
 */
public interface Validatable {
    /**
     * Validates the object and adds messages to the response.
     * This method should NOT throw a ValidationException!
     *
     */
    void validate(ProcessResult response);

    /**
     * Validates the object and throws a ValidationException if it is not valid
     *
     */
    default void ensureValid() throws ValidationException {
        ProcessResult response = new ProcessResult();
        this.validate(response);
        if (!response.isValid()) {
            throw new ValidationException(response);
        }
    }

    default void validate(String contextPrefix, ProcessResult response) {
        ProcessResult contextResult = new ProcessResult();
        this.validate(contextResult);
        contextResult.prefixContextKey(contextPrefix);
        response.addMessages(contextResult);
    }
}
