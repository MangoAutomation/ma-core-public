/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.json.util;

/**
 * @author Matthew Lohbihler
 */
public class ParsePositionTracker {
    private int line = 1;
    private int column = 1;
    private int elementLine;
    private int elementColumn;
    private int characterCount;
    private int markedLine;
    private int markedColumn;
    private int markedCharacterCount;

    public void mark() {
        markedLine = line;
        markedColumn = column;
        markedCharacterCount = characterCount;
    }

    public void reset() {
        line = markedLine;
        column = markedColumn;
        characterCount = markedCharacterCount;
    }

    public void setElementStart() {
        elementLine = line;
        elementColumn = column;
    }

    public void update(char c) {
        if (c == 0xA) { // Line feed
            line++;
            column = 1;
        }
        else
            column++;
        characterCount++;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public int getElementLine() {
        return elementLine;
    }

    public int getElementColumn() {
        return elementColumn;
    }

    public int getCharacterCount() {
        return characterCount;
    }
}
