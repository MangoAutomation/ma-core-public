/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.InvalidArgumentException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.Validatable;
import com.serotonin.util.SerializationHelper;

public class MultistateValue implements Serializable, Validatable {
    private int key;
    private String text;
    private String colour;

    /**
     * Required by DWR. Should not be used otherwise.
     */
    public MultistateValue() {
        // no op
    }

    /**
     * Required by DWR. Should not be used otherwise.
     */
    public void setKey(int key) {
        this.key = key;
    }

    /**
     * Required by DWR. Should not be used otherwise.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Required by DWR. Should not be used otherwise.
     */
    public void setColour(String colour) {
        this.colour = colour;
    }

    public MultistateValue(int key, String text, String colour) {
        this.key = key;
        this.text = text;
        this.colour = colour;
    }

    public int getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public String getColour() {
        return colour;
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(key);
        SerializationHelper.writeSafeUTF(out, text);
        SerializationHelper.writeSafeUTF(out, colour);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            key = in.readInt();
            text = SerializationHelper.readSafeUTF(in);
            colour = SerializationHelper.readSafeUTF(in);
        }
    }
    
    public void validate(ProcessResult result) {
        if(StringUtils.isEmpty(this.text))
            result.addContextualMessage("text", "validate.required");
    
        try {
            if (!StringUtils.isBlank(colour))
                ColorUtils.toColor(colour);
        }
        catch (InvalidArgumentException e) {
            result.addContextualMessage("colour", "systemSettings.validation.invalidColour");
        }
    }
}
