/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.i18n;

/**
 * @author Matthew Lohbihler
 */
public class LocalizableMessageParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public LocalizableMessageParseException() {
        super();
    }

    public LocalizableMessageParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public LocalizableMessageParseException(String message) {
        super(message);
    }

    public LocalizableMessageParseException(Throwable cause) {
        super(cause);
    }
}
