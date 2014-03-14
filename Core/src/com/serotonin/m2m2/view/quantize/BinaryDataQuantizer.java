/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.quantize;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.view.stats.IValueTime;

/**
 * @deprecated use quantize2 classes instead.
 */
@Deprecated
public class BinaryDataQuantizer extends AbstractDataQuantizer {
    private IValueTime startValue;
    private IValueTime zeroValue;
    private IValueTime oneValue;
    private IValueTime endValue;

    public BinaryDataQuantizer(long start, long end, int buckets, DataQuantizerCallback callback) {
        super(start, end, buckets, callback);
    }

    @Override
    protected void periodData(IValueTime vt) {
        if (startValue == null)
            startValue = vt;
        if (vt.getValue().getBooleanValue()) {
            if (oneValue == null)
                oneValue = vt;
        }
        else {
            if (zeroValue == null)
                zeroValue = vt;
        }
        endValue = vt;
    }

    @Override
    protected List<IValueTime> donePeriod(int valueCounter) {
        List<IValueTime> result = new ArrayList<IValueTime>();
        result.add(startValue);
        if (zeroValue != null)
            result.add(zeroValue);
        if (oneValue != null)
            result.add(oneValue);
        result.add(endValue);

        startValue = null;
        zeroValue = null;
        oneValue = null;
        endValue = null;

        return result;
    }
}
