/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.quantize;

import java.util.List;

import com.serotonin.m2m2.view.stats.IValueTime;

/**
 * @deprecated use quantize2 classes instead.
 */
@Deprecated
abstract public class AbstractDataQuantizer {
    private final long start;
    private final int buckets;
    private final long duration;
    private final DataQuantizerCallback callback;

    private int periodCounter;
    private double periodFrom;
    private double periodTo;
    private int valueCounter;

    public AbstractDataQuantizer(long start, long end, int buckets, DataQuantizerCallback callback) {
        this.start = start;
        this.buckets = buckets;
        duration = end - start;
        this.callback = callback;

        periodFrom = start;
        calculatePeriodTo();
    }

    private void calculatePeriodTo() {
        periodTo = periodFrom + ((double) duration) / buckets * ++periodCounter;
    }

    public void data(IValueTime vt) {
        while (vt.getTime() >= periodTo) {
            done();
            periodFrom = periodTo;
            periodTo = start + ((double) duration) / buckets * ++periodCounter;
        }

        valueCounter++;
        periodData(vt);
    }

    public void done() {
        if (valueCounter > 0) {
            callback.quantizedData(donePeriod(valueCounter));
            valueCounter = 0;
        }
    }

    abstract protected void periodData(IValueTime vt);

    abstract protected List<IValueTime> donePeriod(int valueCounter);
}
