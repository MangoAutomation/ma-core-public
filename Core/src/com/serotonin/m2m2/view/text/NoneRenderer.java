/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import java.util.Locale;

import com.serotonin.json.spi.JsonEntity;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.ImplDefinition;

@JsonEntity
public class NoneRenderer extends BaseTextRenderer {
    private static final ImplDefinition definition = new ImplDefinition("textRendererNone", "NONE", "textRenderer.none",
            EnumSet.of(DataTypes.IMAGE));

    public static ImplDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getTypeName() {
        return definition.getName();
    }

    @Override
    public ImplDefinition getDef() {
        return definition;
    }

    public NoneRenderer() {
        // no op
    }

    @Override
    protected String getTextImpl(DataValue value, int hint, Locale locale) {
        return "";
    }

    @Override
    protected String getColourImpl(DataValue value) {
        return null;
    }

    @Override
    public String getChangeSnippetFilename() {
        return null;
    }

    @Override
    public String getSetPointSnippetFilename() {
        return null;
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            // no op
        }
    }

}
