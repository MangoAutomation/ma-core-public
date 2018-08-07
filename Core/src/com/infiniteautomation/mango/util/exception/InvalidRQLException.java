/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

/**
 *
 * @author Terry Packer
 */
public class InvalidRQLException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private String query;
    private String parserMessage;
    
    public InvalidRQLException(String query, String message){
        this.query = query;
        this.parserMessage = message;
    }
    
    public String getQuery(){
        return query;
    }
    
    public String getParserMessage(){
        return parserMessage;
    }
}
