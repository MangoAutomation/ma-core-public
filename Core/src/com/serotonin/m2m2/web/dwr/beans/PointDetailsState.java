/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;

public class PointDetailsState extends BasePointState {
    private String value;
    private String time;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public PointDetailsState clone() {
        try {
            return (PointDetailsState) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public void removeEqualValue(PointDetailsState that) {
        super.removeEqualValue(that);
        if (StringUtils.equals(value, that.value))
            value = null;
        if (StringUtils.equals(time, that.time))
            time = null;
    }

    @Override
    public boolean isEmpty() {
        return value == null && time == null && super.isEmpty();
    }
}
