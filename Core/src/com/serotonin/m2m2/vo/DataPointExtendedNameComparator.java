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
public class DataPointExtendedNameComparator implements Comparator<IDataPoint> {
    public static final DataPointExtendedNameComparator instance = new DataPointExtendedNameComparator();

    @Override
    public int compare(IDataPoint dp1, IDataPoint dp2) {
        if (StringUtils.isBlank(dp1.getExtendedName()))
            return -1;
        return dp1.getExtendedName().compareToIgnoreCase(dp2.getExtendedName());
    }
}
