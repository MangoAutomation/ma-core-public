/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DwrClassConversion {
    private final Class<?> clazz;
    private String converterType = "bean";
    private List<String> includes;
    private List<String> excludes;
    private Map<String,String> parameters;
    
    public DwrClassConversion(Class<?> clazz) {
        this.clazz = clazz;
    }

    public DwrClassConversion(Class<?> clazz, Map<String,String> params) {
        this.clazz = clazz;
        this.parameters = params;
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
    
    public void addParameter(String name, String value){
    	if(parameters == null){
    		parameters = new HashMap<String,String>();
    	}
    	parameters.put(name, value);
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

	/**
	 * @return
	 */
	public Map<String,String> getParameters() {
		return parameters;
	}
}
