package com.serotonin.propertyEditor;

import java.beans.PropertyEditorSupport;
import java.text.DecimalFormat;
import java.text.ParseException;

@Deprecated
public class DecimalFormatEditor extends PropertyEditorSupport {
    protected DecimalFormat format;
    private final boolean hideZero;

    public DecimalFormatEditor(DecimalFormat format, boolean hideZero) {
        this.format = format;
        this.hideZero = hideZero;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().length() == 0)
            setValue(new Double(0));
        else {
            try {
                setValue(new Double(format.parse(text).doubleValue()));
            }
            catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Override
    public String getAsText() {
        if (hideZero) {
            Object value = getValue();
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (d == 0)
                    return "";
            }
        }
        return format.format(getValue());
    }
}
