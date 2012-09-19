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

public class RangeValue implements Serializable {
    private double from;
    private double to;
    private String text;
    private String colour;

    /**
     * Required by DWR. Should not be used otherwise.
     */
    public RangeValue() {
        // no op
    }

    /**
     * Required by DWR. Should not be used otherwise.
     */
    public void setFrom(double from) {
        this.from = from;
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

    /**
     * Required by DWR. Should not be used otherwise.
     */
    public void setTo(double to) {
        this.to = to;
    }

    public RangeValue(double from, double to, String text, String colour) {
        this.from = from;
        this.to = to;
        this.text = text;
        this.colour = colour;
    }

    boolean contains(double d) {
        return d >= from && d <= to;
    }

    public double getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }

    public double getTo() {
        return to;
    }

    public String getColour() {
        return colour;
    }

    //
    // /
    // / Serialization
    // /
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeDouble(from);
        out.writeDouble(to);
        SerializationHelper.writeSafeUTF(out, text);
        SerializationHelper.writeSafeUTF(out, colour);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            from = in.readDouble();
            to = in.readDouble();
            text = SerializationHelper.readSafeUTF(in);
            colour = SerializationHelper.readSafeUTF(in);
        }
    }
}
