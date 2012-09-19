/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
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
