/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 *
 * @author Terry Packer
 */
public class IllegalStateRestException extends AbstractRestV2Exception {

    private static final long serialVersionUID = 1L;

    public IllegalStateRestException(TranslatableMessage message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.BAD_REQUEST, message, cause);
    }

    public IllegalStateRestException(TranslatableMessage message) {
        super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.BAD_REQUEST, message);
    }

}
