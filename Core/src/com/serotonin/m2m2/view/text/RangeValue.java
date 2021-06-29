/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.InvalidArgumentException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.Validatable;
import com.serotonin.util.SerializationHelper;

public class RangeValue implements Serializable, Comparable<RangeValue>, Validatable {
    private double from;
    private double to;
    private String text;
    private String colour;

    private static final Pattern VALUE_PATTERN = Pattern.compile("\\$\\{value\\}");

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

    String formatText(String number) {
        String escapedReplacement = Matcher.quoteReplacement(number);
        return VALUE_PATTERN.matcher(this.text).replaceAll(escapedReplacement);
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

    @Override
    public int compareTo(RangeValue o) {
        double thisTo = this.getTo();
        double otherTo = o.getTo();

        if (thisTo < otherTo) {
            return -1;
        } else if (thisTo > otherTo) {
            return 1;
        } else {
            double thisFrom = this.getFrom();
            double otherFrom = o.getFrom();

            if (thisFrom < otherFrom) {
                return -1;
            } else if (thisFrom > otherFrom) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    
    @Override
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
