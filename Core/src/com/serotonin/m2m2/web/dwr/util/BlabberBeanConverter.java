/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.util;

import java.util.List;

import org.directwebremoting.convert.BeanConverter;

public class BlabberBeanConverter extends BeanConverter {
    @SuppressWarnings("unchecked")
    public List<String> getExclusions() {
        return exclusions;
    }

    @SuppressWarnings("unchecked")
    public List<String> getInclusions() {
        return inclusions;
    }
}
