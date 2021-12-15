/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Thrown when a VO object can't be found.
 *
 * This exception is caught and converted to a NotFoundRestException for REST. See the MangoSpringExceptionHandler.
 *
 * @author Jared Wiltshire
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
