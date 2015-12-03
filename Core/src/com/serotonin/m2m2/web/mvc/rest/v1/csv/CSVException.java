/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.io.IOException;

/**
 * @author Terry Packer
 *
 */
public class CSVException extends IOException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	/**
     * Constructs an {@code CSVException} with {@code null}
     * as its error detail message.
     */
    public CSVException() {
        super();
    }

    /**
     * Constructs an {@code CSVException} with the specified detail message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     */
    public CSVException(String message) {
        super(message);
    }
	
}
