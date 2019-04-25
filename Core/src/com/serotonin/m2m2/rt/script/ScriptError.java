/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

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
     * @param e
     * @return
     */
    public static ScriptError create(ScriptException e) {
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
        }{
            Matcher matcher = PATTERN.matcher(message);
            if (matcher.find())
                message = matcher.group(3);
        }
        return new ScriptError(message, 
                e.getLineNumber() == -1 ? null : e.getLineNumber(),
                e.getColumnNumber() == -1 ? null : e.getColumnNumber(),
                t);       
    }

    /**
     * Generic wrap all for things like ClassNotFoundException etc. that have no line numbers
     * 
     * @param cause
     * @return
     */
    public static ScriptError create(Throwable cause) {
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
