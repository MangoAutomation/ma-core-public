/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 *
 * @author Terry Packer
 */
public class ModuleRestV2Exception extends AbstractRestV2Exception{
    private static final long serialVersionUID = 1L;

    public ModuleRestV2Exception(HttpStatus httpCode, Throwable cause) {
        super(httpCode, cause);
    }

    public ModuleRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, Throwable cause) {
        super(httpCode, mangoCode, cause);
        if (mangoCode == null || mangoCode.getCode() >= 1000) {
            throw new IllegalArgumentException ("Module status codes must be < 1000");
        }
    }

    public ModuleRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, TranslatableMessage message) {
        super(httpCode, mangoCode, message);
        if (mangoCode == null || mangoCode.getCode() >= 1000) {
            throw new IllegalArgumentException ("Module status codes must be < 1000");
        }
    }

    public ModuleRestV2Exception(HttpStatus httpCode, TranslatableMessage message) {
        super(httpCode, null, message);
    }
}
