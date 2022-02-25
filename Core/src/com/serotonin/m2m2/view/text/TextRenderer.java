/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.Serializable;
import java.util.Locale;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.ImplDefinition;

public interface TextRenderer extends Serializable {

    /**
     * Do not render the value. Just return the java-formatted version of the value.
     */
    int HINT_RAW = 1;
    /**
     * Render the value according to the full functionality of the renderer.
     */
    int HINT_FULL = 2;
    /**
     * Render the value in a way that does not generalize. Currently only used to prevent analog range renderers from
     * obfuscating a numeric into a descriptor.
     */
    int HINT_SPECIFIC = 4;
    /**
     * Do not convert when rendering
     */
    int HINT_NO_CONVERT = 8;
    
    String UNKNOWN_VALUE = "(n/a)";

    String getText(int hint, Locale locale);

    String getText(PointValueTime valueTime, int hint, Locale locale);
    
    default String getText(PointValueTime valueTime, int hint) {
        return getText(valueTime, hint, getLocale());
    }

    String getText(DataValue value, int hint, Locale locale);
    
    default String getText(DataValue value, int hint) {
        return getText(value, hint, getLocale());
    }

    String getText(double value, int hint, Locale locale);
    
    default String getText(double value, int hint) {
        return getText(value, hint, getLocale());
    }

    String getText(int value, int hint, Locale locale);
    
    default String getText(int value, int hint) {
        return getText(value, hint, getLocale());
    }

    String getText(boolean value, int hint, Locale locale);

    default String getText(boolean value, int hint) {
        return getText(value, hint, getLocale());
    }

    String getText(String value, int hint, Locale locale);
    
    default String getText(String value, int hint) {
        return getText(value, hint, getLocale());
    }

    String getColour();

    String getColour(PointValueTime valueTime);

    String getColour(DataValue value);

    String getColour(double value);

    String getColour(int value);

    String getColour(boolean value);

    ImplDefinition getDef();

    DataValue parseText(String s, DataType dataType);
    
    /**
     * Validate the settings of the renderer
     */
    default void validate(ProcessResult result, DataType sourcePointDataType) {
        if(!getDef().supports(sourcePointDataType))
            result.addContextualMessage("dataType", "validate.text.incompatible");
    }

    default Locale getLocale() {
        return Common.getUser().getLocaleObject();
    }
    
}
