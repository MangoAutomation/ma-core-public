/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.export;

/**
 * @author Matthew Lohbihler
 */
public class CsvWriter {
    private static final String CRLF = "\r\n";

    private final StringBuilder sb = new StringBuilder();

    public String encodeRow(String[] data) {
        sb.setLength(0);

        boolean first = true;
        for (String s : data) {
            if (first)
                first = false;
            else
                sb.append(',');

            if (s != null)
                sb.append(encodeValue(s));
        }

        sb.append(CRLF);

        return sb.toString();
    }

    public String encodeValue(String fieldValue) {
        if (fieldValue == null)
            fieldValue = "";

        boolean needsQuotes = false;

        // Fields with embedded commas must be delimited with double-quote characters.
        if (fieldValue.indexOf(',') != -1)
            needsQuotes = true;

        // Fields that contain double quote characters must be surounded by double-quotes,
        // and the embedded double-quotes must each be represented by a pair of consecutive
        // double quotes.
        if (fieldValue.indexOf('"') != -1) {
            needsQuotes = true;
            fieldValue = fieldValue.replaceAll("\"", "\"\"");
        }

        // A field that contains embedded line-breaks must be surounded by double-quotes
        if (fieldValue.indexOf('\n') != -1 || fieldValue.indexOf('\r') != -1)
            needsQuotes = true;

        // Fields with leading or trailing spaces must be delimited with double-quote characters.
        if (fieldValue.startsWith(" ") || fieldValue.endsWith(" "))
            needsQuotes = true;

        if (needsQuotes)
            fieldValue = '"' + fieldValue + '"';

        return fieldValue;
    }
}
