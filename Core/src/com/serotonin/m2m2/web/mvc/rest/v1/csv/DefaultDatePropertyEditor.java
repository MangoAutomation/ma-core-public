package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * A simple DatePropertyEditor that will use DateFormat.getInstance() for reading and writing dates.
 * The exact format will depend on the the locale.
 * 
 * @author Staffan Friberg
 */
public class DefaultDatePropertyEditor extends CSVPropertyEditor {

    Date date;

    @Override
    public void setValue(Object value) {
        this.date = (Date) value;
    }

    @Override
    public Object getValue() {
        return date;
    }

    @Override
    public String getAsText() {
        return date == null ? "" : DateFormat.getInstance().format(date);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            date = DateFormat.getInstance().parse(text);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
