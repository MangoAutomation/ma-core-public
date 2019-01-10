/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.regex.Pattern;

import javax.script.ScriptException;

import com.serotonin.util.StringUtils;

/**
 * 
 * @author Terry Packer
 *
 */
public class ScriptError extends Exception {
    private static final long serialVersionUID = 1L;
    private static final Pattern PATTERN = Pattern.compile("(.*?): (.*?) \\(.*?\\)");

    public static ScriptError create(ScriptException sex) {
        String m = sex.getMessage();
        return new ScriptError(StringUtils.findGroup(PATTERN, m, 2), StringUtils.findGroup(PATTERN, m, 1),
                sex.getLineNumber(), sex.getColumnNumber(), sex);
    }

    private final String exClass;
    private final int lineNumber;
    private final int columnNumber;

    ScriptError(String message, String exClass, int lineNumber, int columnNumber, ScriptException cause) {
        super(message, cause);
        this.exClass = exClass;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getExClass() {
        return exClass;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}
