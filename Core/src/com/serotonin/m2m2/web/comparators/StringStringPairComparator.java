/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.comparators;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.serotonin.db.pair.StringStringPair;

public class StringStringPairComparator implements Comparator<StringStringPair> {
    private final static StringStringPairComparator INSTANCE = new StringStringPairComparator();

    public static void sort(List<StringStringPair> list) {
        Collections.sort(list, INSTANCE);
    }

    @Override
    public int compare(StringStringPair ssp1, StringStringPair ssp2) {
        return ssp1.getValue().compareToIgnoreCase(ssp2.getValue());
    }
}
