/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

/**
 * @author Matthew Lohbihler
 */
public class IpWhiteListException extends Exception {
    private static final long serialVersionUID = -1;

    public IpWhiteListException(String message) {
        super(message);
    }
}
