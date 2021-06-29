/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * 
 * @author Matthew Lohbihler, Terry Packer
 *
 */
public class ScriptError extends TranslatableException {
    private static final long serialVersionUID = 1L;
    private static final Pattern PATTERN = Pattern.compile("<eval>(.*?):(.*?) ([\\s\\S]*)");

    /**
     * Extract a useful message and line/col info for the exception
     * 
     * @param e
     * @param wrapped - was this script wrapped within a function calls (hidden to submitting user)
     * @return
     */
    public static ScriptError create(ScriptException e, boolean wrapped) {        
        Throwable t = e;
        while (t.getCause() != null && t.getCause().getMessage() != null) {
            t = t.getCause();
        }
        String message;

        message = t.getMessage();
        if(message == null && t instanceof NullPointerException)
            message = "null pointer exception";
        else if(message == null){
            message = e.getMessage(); //Fallback
        }else {
            Matcher matcher = PATTERN.matcher(message);
            if (matcher.find())
                message = matcher.group(3);
        }
        
        //Replace any message context around our prefix/suffix of wrapped scripts
        message = message.replace(MangoJavaScriptService.SCRIPT_PREFIX, "");
        message = message.replace(MangoJavaScriptService.SCRIPT_SUFFIX, "");
        
        //Convert the line numbers
        Integer line = e.getLineNumber() == -1 ? null : e.getLineNumber();
        Integer column = e.getColumnNumber() == -1 ? null : e.getColumnNumber();
        if(wrapped) {
            if(column != null && line != null && line == 1) {
                //adjust the column by the amound of our prefix
                column = column - MangoJavaScriptService.SCRIPT_PREFIX.length();
            }
        }
        
        return new ScriptError(message, 
                line,
                column,
                t);       
    }

    /**
     * Generic wrap all for things like ClassNotFoundException etc. that have no line numbers
     * 
     * @param cause
     * @return
     */
    public static ScriptError createFromThrowable(Throwable cause) {
        if(cause == null)
            return new ScriptError("null pointer exception", null, null, null);
        else
            return new ScriptError(cause.getClass().getName() + ": " + cause.getMessage(), null, null, cause);
    }
    
    public static TranslatableMessage createMessage(String message, Integer lineNumber, Integer columnNumber) {
        if(lineNumber == null) {
            return  new TranslatableMessage("literal", message);
        } else {
            if (columnNumber == null)
                return new TranslatableMessage("javascript.validate.rhinoException", message, lineNumber);
            else
                return new TranslatableMessage("javascript.validate.rhinoExceptionCol", message, lineNumber, columnNumber);
        }
    }
    
    private final Integer lineNumber;
    private final Integer columnNumber;

    ScriptError(TranslatableMessage message){
        super(message);
        this.lineNumber = null;
        this.columnNumber = null;
    }
    
    ScriptError(String message, Integer lineNumber, Integer columnNumber, Throwable cause) {
        super(createMessage(message, lineNumber, columnNumber), cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }

}
