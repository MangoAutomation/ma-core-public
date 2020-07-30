/*
 * Copyright (C) 2017 Infinite Automation Systems Inc. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Thrown when a VO object can't be found.
 *
 * This exception is caught and converted to a {@link com.infiniteautomation.mango.rest.latest.exception.NotFoundRestException} for REST.
 *
 * @author Jared Wiltshire
 * @see com.infiniteautomation.mango.rest.latest.MangoSpringExceptionHandler
 */
public class NotFoundException extends TranslatableRuntimeException {
    private static final long serialVersionUID = 1L;

    public NotFoundException() {
        super(new TranslatableMessage("translatableException.notFound"));
    }

    public NotFoundException(Throwable cause) {
        super(new TranslatableMessage("translatableException.notFound"), cause);
    }
}
