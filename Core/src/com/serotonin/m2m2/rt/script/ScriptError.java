/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

/**
 * 
 * @author Matthew Lohbihler, Terry Packer
 *
 */
public class ScriptError extends Exception {
    private static final long serialVersionUID = 1L;
    private static final Pattern PATTERN = Pattern.compile("(.*?):(.*?) ([\\s\\S]*)");

    public static ScriptError create(ScriptException e) {
        Throwable t = e;
        while (t.getCause() != null)
            t = t.getCause();
        String message;

        message = t.getMessage();
        if(message == null)
            message = "null pointer exception";

        Matcher matcher = PATTERN.matcher(message);
        if (matcher.find())
            message = matcher.group(3);
        return new ScriptError(message, 
                e.getLineNumber() == -1 ? null : e.getLineNumber(),
                e.getColumnNumber() == -1 ? null : e.getColumnNumber(),
                t);
    }

    private final Integer lineNumber;
    private final Integer columnNumber;

    ScriptError(String message, Integer lineNumber, Integer columnNumber, Throwable cause) {
        super(message, cause);
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
