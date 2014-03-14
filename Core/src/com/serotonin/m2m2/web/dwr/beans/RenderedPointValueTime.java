/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

/**
 * @author Matthew Lohbihler
 */
public class RenderedPointValueTime {
    private String value;
    private String time;
    private String annotation;

    public RenderedPointValueTime() {
        // no op
    }

    public RenderedPointValueTime(String value, String time, String annotation) {
        this.value = value;
        this.time = time;
        this.annotation = annotation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
}
