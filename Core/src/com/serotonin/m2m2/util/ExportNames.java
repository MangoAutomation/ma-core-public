/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.serotonin.db.pair.StringStringPair;

/**
 * @author Matthew Lohbihler
 */
public class ExportNames {
    private final List<Element> elements = new ArrayList<Element>();

    public void addElement(String code) {
        elements.add(new Element(code, null));
    }

    public void addElement(String code, String key) {
        elements.add(new Element(code, key));
    }

    public String getKey(String code) {
        Element e = getElement(code);
        if (e == null)
            return null;
        return e.key;
    }

    public boolean hasCode(String code, String... excludeCodes) {
        for (int i = 0; i < elements.size(); i++) {
            String ecode = elements.get(i).code;
            if (ecode.equalsIgnoreCase(code) && !ArrayUtils.contains(excludeCodes, ecode))
                return true;
        }
        return false;
    }

    public List<String> getCodeList(String... excludeCodes) {
        List<String> result = new ArrayList<String>(elements.size());
        for (Element e : elements) {
            if (!ArrayUtils.contains(excludeCodes, e.code))
                result.add(e.code);
        }
        return result;
    }

    private Element getElement(String code) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).code.equalsIgnoreCase(code))
                return elements.get(i);
        }
        return null;
    }

    public int size() {
        return elements.size();
    }

    public String getCode(int index) {
        return elements.get(index).code;
    }

    public List<StringStringPair> getCodeKeys(String... excludeCodes) {
        List<StringStringPair> result = new ArrayList<StringStringPair>(elements.size());
        for (Element e : elements) {
            if (!ArrayUtils.contains(excludeCodes, e.code))
                result.add(new StringStringPair(e.code, e.key));
        }
        return result;
    }

    class Element {
        final String code;
        final String key;

        Element(String code, String key) {
            this.code = code;
            this.key = key;
        }
    }
}
