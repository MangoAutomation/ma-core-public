/*
 * Copyright (C) 2017 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.vo;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * @author Jared Wiltshire
 */
public interface Validatable {
    /**
     * Validates the object and adds messages to the response.
     * This method should NOT throw a ValidationException!
     *
     * @param response
     */
    public void validate(ProcessResult response);

    /**
     * Validates the object and throws a ValidationException if it is not valid
     *
     * @throws ValidationException
     */
    public default void ensureValid() throws ValidationException {
        ProcessResult response = new ProcessResult();
        this.validate(response);
        if (!response.isValid()) {
            throw new ValidationException(response);
        }
    }

    public default void validate(String contextPrefix, ProcessResult response) {
        ProcessResult contextResult = new ProcessResult();
        this.validate(contextResult);
        for (ProcessMessage msg : contextResult.getMessages()) {
            String contextKey = msg.getContextKey();
            if (contextKey != null) {
                msg.setContextKey(contextPrefix + "." + contextKey);
            }
            response.addMessage(msg);
        }
    }
}
