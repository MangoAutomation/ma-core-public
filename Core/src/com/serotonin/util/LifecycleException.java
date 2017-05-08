/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

/**
 * @author Matthew Lohbihler
 */
public class LifecycleException extends Exception {
    static final long serialVersionUID = -1;

    public LifecycleException() {
        super();
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(Throwable cause) {
        super(cause);
    }
}
