/*
 * Copyright (C) 2017 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.vo.exception;

/**
 * Thrown when a VO object can't be found.
 * 
 * This exception is caught and converted to a {@link com.infiniteautomation.mango.rest.v2.exception.NotFoundRestException} for REST.
 * 
 * @author Jared Wiltshire
 * @see com.serotonin.m2m2.web.mvc.spring.exception.MangoSpringExceptionHandler
 */
public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotFoundException() {
        super("Not found");
    }
    
    public NotFoundException(Throwable cause) {
        super("Not found", cause);
    }
}
