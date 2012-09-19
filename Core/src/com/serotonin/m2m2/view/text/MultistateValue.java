/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.serotonin.util.SerializationHelper;

public class MultistateValue implements Serializable {
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
}
