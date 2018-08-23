/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public class BadRequestException extends AbstractRestV2Exception {
    private static final long serialVersionUID = 1L;

    public BadRequestException() {
        super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.BAD_REQUEST);
    }

    public BadRequestException(Throwable cause) {
        super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.BAD_REQUEST, cause);
    }

    public BadRequestException(TranslatableMessage message) {
        super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(TranslatableMessage message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.BAD_REQUEST, message, cause);
    }
}
