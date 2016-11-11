package com.serotonin.m2m2.rt.script;

import javax.script.ScriptException;

public class ScriptError extends Exception {
    private static final long serialVersionUID = 1L;

    public static ScriptError create(Exception e) {
    	ScriptException sex = CompiledScriptExecutor.createScriptError(e);
    	return new ScriptError(sex.getMessage(), sex.getFileName(), sex.getLineNumber(), sex.getColumnNumber(),sex);
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
