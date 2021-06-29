/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

/**
 *
 * @author Terry Packer
 */
public class InvalidRQLException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String query;

    public InvalidRQLException(Throwable cause, String query) {
        super(cause);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
