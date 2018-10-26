/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

public class RQLToSQLParseException extends RuntimeException{

    private static final long serialVersionUID = 1L;

    public RQLToSQLParseException(String message){
        super(message);
    }

    public RQLToSQLParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
