/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class DataPointNameComparator implements Comparator<DataPointVO> {
    public static final DataPointNameComparator instance = new DataPointNameComparator();

    public int compare(DataPointVO dp1, DataPointVO dp2) {
        if (StringUtils.isBlank(dp1.getName()))
            return -1;
        return dp1.getName().compareToIgnoreCase(dp2.getName());
    }
}
