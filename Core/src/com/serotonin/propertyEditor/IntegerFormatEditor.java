package com.serotonin.propertyEditor;

import java.text.DecimalFormat;
import java.text.ParseException;

@Deprecated
public class IntegerFormatEditor extends DecimalFormatEditor {
    public IntegerFormatEditor(DecimalFormat format, boolean hideZero) {
        super(format, hideZero);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().length() == 0)
            setValue(new Integer(0));
        else {
            try {
                setValue(new Integer(format.parse(text).intValue()));
            }
            catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
