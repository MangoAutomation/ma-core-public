/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

/**
 * @author Jared Wiltshire
 */
public class SourceLocation {
    private final String fileName;
    private final Integer lineNumber;
    private final Integer columnNumber;

    public SourceLocation(String fileName, Integer lineNumber, Integer columnNumber) {
        super();
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }
}