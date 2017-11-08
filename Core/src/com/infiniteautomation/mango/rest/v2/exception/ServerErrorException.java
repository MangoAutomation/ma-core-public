/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public class ServerErrorException extends AbstractRestV2Exception {
    private static final long serialVersionUID = 1L;
    
    public ServerErrorException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ServerErrorException(Exception e) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, null, e);
    }

    public ServerErrorException(TranslatableMessage message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, null, message);
    }
    
    public ServerErrorException(TranslatableMessage message, Exception e) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, null, message, e);
    }
}
