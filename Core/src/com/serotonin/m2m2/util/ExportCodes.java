/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.serotonin.db.pair.IntStringPair;

/**
 * @author Matthew Lohbihler
 */
public class ExportCodes {
    private final List<Element> elements = new ArrayList<Element>();

    public void addElement(int id, String code) {
        elements.add(new Element(id, code, null));
    }

    public void addElement(int id, String code, String key) {
        elements.add(new Element(id, code, key));
    }

    public String getCode(int id) {
        Element e = getElement(id);
        if (e == null)
            return null;
        return e.code;
    }

    public String getKey(int id) {
        Element e = getElement(id);
        if (e == null)
            return null;
        return e.key;
    }

    public int getId(String code, int... excludeIds) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).code.equalsIgnoreCase(code) && !ArrayUtils.contains(excludeIds, elements.get(i).id))
                return elements.get(i).id;
        }
        return -1;
    }

    public boolean isValidId(int id, int... excludeIds) {
        for (int i = 0; i < elements.size(); i++) {
            int eid = elements.get(i).id;
            if (!ArrayUtils.contains(excludeIds, eid) && eid == id)
                return true;
        }
        return false;
    }

    public List<String> getCodeList(int... excludeIds) {
        List<String> result = new ArrayList<String>(elements.size());
        for (Element e : elements) {
            if (!ArrayUtils.contains(excludeIds, e.id))
                result.add(e.code);
        }
        return result;
    }

    private Element getElement(int id) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).id == id)
                return elements.get(i);
        }
        return null;
    }

    public int size() {
        return elements.size();
    }

    public int getId(int index) {
        return elements.get(index).id;
    }

    public List<IntStringPair> getIdKeys(int... excludeIds) {
        List<IntStringPair> result = new ArrayList<IntStringPair>(elements.size());
        for (Element e : elements) {
            if (!ArrayUtils.contains(excludeIds, e.id))
                result.add(new IntStringPair(e.id, e.key));
        }
        return result;
    }
    
    public List<Element> getElements() {
        return elements;
    }
    
    public static class Element {
        
        final int id;
        final String code;
        final String key;

        Element(int id, String code, String key) {
            this.id = id;
            this.code = code;
            this.key = key;
        }

        public int getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getKey() {
            return key;
        }
        
    }
}
