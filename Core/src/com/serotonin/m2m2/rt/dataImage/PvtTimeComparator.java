/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Comparator;

/**
 * @author Matthew Lohbihler
 */
public class PvtTimeComparator implements Comparator<PointValueTime> {
    @Override
    public int compare(PointValueTime o1, PointValueTime o2) {
        long diff = o1.getTime() - o2.getTime();
        if (diff < 0)
            return -1;
        if (diff > 0)
            return 1;
        return 0;
    }
}
