/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
