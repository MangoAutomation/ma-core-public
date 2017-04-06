/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.json;

/**
 * A basic JSON exception.
 * 
 * @author Matthew Lohbihler
 */
public class JsonException extends Exception {
    private static final long serialVersionUID = 1L;

    public JsonException() {
        super();
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonException(String message) {
        super(message);
    }

    public JsonException(Throwable cause) {
        super(cause);
    }
}
