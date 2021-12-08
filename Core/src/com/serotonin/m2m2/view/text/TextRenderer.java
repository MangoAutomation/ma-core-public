/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.Serializable;
import java.util.Locale;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.ImplDefinition;

public interface TextRenderer extends Serializable {
    public static final int TYPE_ANALOG = 1;
    public static final int TYPE_BINARY = 2;
    public static final int TYPE_MULTISTATE = 3;
    public static final int TYPE_PLAIN = 4;
    public static final int TYPE_RANGE = 5;

    /**
     * Do not render the value. Just return the java-formatted version of the value.
     */
    public static final int HINT_RAW = 1;
    /**
     * Render the value according to the full functionality of the renderer.
     */
    public static final int HINT_FULL = 2;
    /**
     * Render the value in a way that does not generalize. Currently only used to prevent analog range renderers from
     * obfuscating a numeric into a descriptor.
     */
    public static final int HINT_SPECIFIC = 4;
    /**
     * Do not convert when rendering
     */
    public static final int HINT_NO_CONVERT = 8;
    
    public static final String UNKNOWN_VALUE = "(n/a)";

    public String getText(int hint, Locale locale);
    
    default public String getText(int hint) {
        return this.getText(hint, Common.getLocale());
    }

    public String getText(PointValueTime valueTime, int hint, Locale locale);
    
    default public String getText(PointValueTime valueTime, int hint) {
        return getText(valueTime, hint, Common.getLocale());
    }

    public String getText(DataValue value, int hint, Locale locale);
    
    default public String getText(DataValue value, int hint) {
        return getText(value, hint, Common.getLocale());
    }

    public String getText(double value, int hint, Locale locale);
    
    default public String getText(double value, int hint) {
        return getText(value, hint, Common.getLocale());
    }

    public String getText(int value, int hint, Locale locale);
    
    default public String getText(int value, int hint) {
        return getText(value, hint, Common.getLocale());
    }

    public String getText(boolean value, int hint, Locale locale);

    default public String getText(boolean value, int hint) {
        return getText(value, hint, Common.getLocale());
    }

    public String getText(String value, int hint, Locale locale);
    
    default public String getText(String value, int hint) {
        return getText(value, hint, Common.getLocale());
    }

    public String getMetaText();

    public String getColour();

    public String getColour(PointValueTime valueTime);

    public String getColour(DataValue value);

    public String getColour(double value);

    public String getColour(int value);

    public String getColour(boolean value);

    public String getColour(String value);

    public String getTypeName();

    public ImplDefinition getDef();

    public String getChangeSnippetFilename();

    public String getSetPointSnippetFilename();

    public DataValue parseText(String s, DataTypes dataType);
    
    /**
     * Validate the settings of the renderer
     */
    public default void validate(ProcessResult result, DataTypes sourcePointDataType) {
        if(!getDef().supports(sourcePointDataType))
            result.addContextualMessage("dataType", "validate.text.incompatible");
    }
    
}
