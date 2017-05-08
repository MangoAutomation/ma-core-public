/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.i18n;

/**
 * @author Matthew Lohbihler
 */
public class LocalizableException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private LocalizableMessage localizableMessage = null;
    
    public LocalizableException() {
        super();
    }

    public LocalizableException(LocalizableMessage message, Throwable cause) {
        super(cause);
        this.localizableMessage = message;
    }

    public LocalizableException(LocalizableMessage message) {
        this.localizableMessage = message;
    }

    public LocalizableException(Throwable cause) {
        super(cause);
    }

    public LocalizableMessage getLocalizableMessage() {
        return localizableMessage;
    }
}
