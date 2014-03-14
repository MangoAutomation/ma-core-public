/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.comparators;

import java.util.Comparator;

abstract public class BaseComparator<T> implements Comparator<T> {
    protected int sortType;
    protected boolean descending;

    public boolean canSort() {
        return sortType != 0;
    }
}
