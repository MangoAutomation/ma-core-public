/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt;

/**
 * @author Matthew Lohbihler
 */
public class RTException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RTException() {
        super();
    }

    public RTException(String message, Throwable cause) {
        super(message, cause);
    }

    public RTException(String message) {
        super(message);
    }

    public RTException(Throwable cause) {
        super(cause);
    }
}
