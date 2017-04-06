/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.json;

import com.serotonin.json.util.ParsePositionTracker;

/**
 * Provides information on the location within a JSON document where a read exception occurred.
 * 
 * @author Matthew Lohbihler
 */
public class JsonParseException extends JsonException {
    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public JsonParseException(String message, Throwable cause, ParsePositionTracker tracker) {
        super(message, cause);
        this.line = tracker.getElementLine();
        this.column = tracker.getElementColumn();
    }

    public JsonParseException(String message, ParsePositionTracker tracker, boolean element) {
        super(message);
        if (element) {
            this.line = tracker.getElementLine();
            this.column = tracker.getElementColumn();
        }
        else {
            this.line = tracker.getLine();
            this.column = tracker.getColumn();
        }
    }

    public JsonParseException(Throwable cause, ParsePositionTracker tracker) {
        super(cause);
        this.line = tracker.getElementLine();
        this.column = tracker.getElementColumn();
    }

    /**
     * @return the line in the JSON document where the error occurred.
     */
    public int getLine() {
        return line;
    }

    /**
     * @return the column in the JSON document where the error occurred.
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return a message describing the parsing error that occurred including the line and column in the JSON document.
     */
    @Override
    public String getMessage() {
        return "line=" + line + ", column=" + column + ": " + super.getMessage();
    }
}
