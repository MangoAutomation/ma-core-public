/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.util;

import java.util.ArrayList;
import java.util.List;

public class DwrClassConversion {
    private final Class<?> clazz;
    private String converterType = "bean";
    private List<String> includes;
    private List<String> excludes;

    public DwrClassConversion(Class<?> clazz) {
        this.clazz = clazz;
    }

    public void addInclude(String s) {
        if (includes == null)
            includes = new ArrayList<String>();
        includes.add(s);
    }

    public void addExclude(String s) {
        if (excludes == null)
            excludes = new ArrayList<String>();
        excludes.add(s);
    }

    public String getConverterType() {
        return converterType;
    }

    public void setConverterType(String converterType) {
        this.converterType = converterType;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }
}
