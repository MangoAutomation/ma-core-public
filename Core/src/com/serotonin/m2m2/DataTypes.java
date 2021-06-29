/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.ExportCodes;

public class DataTypes {
    public static final int UNKNOWN = 0;
    public static final int BINARY = 1;
    public static final int MULTISTATE = 2;
    public static final int NUMERIC = 3;
    public static final int ALPHANUMERIC = 4;
    public static final int IMAGE = 5;

    public static TranslatableMessage getDataTypeMessage(int id) {
        switch (id) {
        case BINARY:
            return new TranslatableMessage("common.dataTypes.binary");
        case MULTISTATE:
            return new TranslatableMessage("common.dataTypes.multistate");
        case NUMERIC:
            return new TranslatableMessage("common.dataTypes.numeric");
        case ALPHANUMERIC:
            return new TranslatableMessage("common.dataTypes.alphanumeric");
        case IMAGE:
            return new TranslatableMessage("common.dataTypes.image");
        }
        return new TranslatableMessage("common.unknown");
    }

    public static final ExportCodes CODES = new ExportCodes();
    static {
        CODES.addElement(BINARY, "BINARY");
        CODES.addElement(MULTISTATE, "MULTISTATE");
        CODES.addElement(NUMERIC, "NUMERIC");
        CODES.addElement(ALPHANUMERIC, "ALPHANUMERIC");
        CODES.addElement(IMAGE, "IMAGE");
    }

    public static int getDataType(DataValue value) {
        if (value == null)
            return UNKNOWN;
        return value.getDataType();
    }

    public static String valueToString(DataValue value) {
        if (value == null)
            return null;
        return value.toString();
    }
}
