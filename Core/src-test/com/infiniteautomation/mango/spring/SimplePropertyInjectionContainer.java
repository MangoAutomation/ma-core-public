/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Terry Packer
 *
 */
@Component
public class SimplePropertyInjectionContainer {

    private List<String> injectedEmptyStringArray;
    private List<String> injectedStringArray;
    private List<Integer> injectedIntegerArray;
    private boolean injectedBoolean;
    private String injectedString;
    private Integer injectedInteger;
    
    @Autowired
    public void setProperties(
            @Value("${test.injectedStringArray}") List<String> injectedStringArray,
            @Value("${test.injectedEmptyStringArray:}") List<String> injectedEmptyStringArray,
            @Value("${test.injectedIntegerArray}") List<Integer> injectedIntegerArray,
            @Value("${test.injectedBoolean:false}") Boolean injectedBoolean,
            @Value("${test.injectedString:test}") String injectedString,
            @Value("${test.injectedInteger:99}") Integer injectedInteger
            ) {
        this.injectedStringArray = injectedStringArray;
        this.injectedEmptyStringArray = injectedEmptyStringArray;
        this.injectedIntegerArray = injectedIntegerArray;
        this.injectedBoolean = injectedBoolean;
        this.injectedString = injectedString;
        this.injectedInteger = injectedInteger;
    }

    /**
     * @return the injectedEmptyStringArray
     */
    public List<String> getInjectedEmptyStringArray() {
        return injectedEmptyStringArray;
    }

    /**
     * @param injectedEmptyStringArray the injectedEmptyStringArray to set
     */
    public void setInjectedEmptyStringArray(List<String> injectedEmptyStringArray) {
        this.injectedEmptyStringArray = injectedEmptyStringArray;
    }

    /**
     * @return the injectedStringArray
     */
    public List<String> getInjectedStringArray() {
        return injectedStringArray;
    }

    /**
     * @param injectedStringArray the injectedStringArray to set
     */
    public void setInjectedStringArray(List<String> injectedStringArray) {
        this.injectedStringArray = injectedStringArray;
    }

    /**
     * @return the injectedIntegerArray
     */
    public List<Integer> getInjectedIntegerArray() {
        return injectedIntegerArray;
    }

    /**
     * @param injectedIntegerArray the injectedIntegerArray to set
     */
    public void setInjectedIntegerArray(List<Integer> injectedIntegerArray) {
        this.injectedIntegerArray = injectedIntegerArray;
    }

    /**
     * @return the injectedBoolean
     */
    public boolean isInjectedBoolean() {
        return injectedBoolean;
    }

    /**
     * @param injectedBoolean the injectedBoolean to set
     */
    public void setInjectedBoolean(boolean injectedBoolean) {
        this.injectedBoolean = injectedBoolean;
    }

    /**
     * @return the injectedString
     */
    public String getInjectedString() {
        return injectedString;
    }

    /**
     * @param injectedString the injectedString to set
     */
    public void setInjectedString(String injectedString) {
        this.injectedString = injectedString;
    }

    /**
     * @return the injectedInteger
     */
    public Integer getInjectedInteger() {
        return injectedInteger;
    }

    /**
     * @param injectedInteger the injectedInteger to set
     */
    public void setInjectedInteger(Integer injectedInteger) {
        this.injectedInteger = injectedInteger;
    }
    
    
}
