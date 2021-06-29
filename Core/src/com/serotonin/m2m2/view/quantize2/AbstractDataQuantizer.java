/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.quantize2;

import java.util.List;

import org.joda.time.DateTime;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;

@Deprecated //Use com.infiniteautomation.mango.quantize class instead
abstract public class AbstractDataQuantizer {
    private final BucketCalculator bucketCalculator;
    private final long startTime;

    private DateTime periodFrom;
    private DateTime periodTo;
    private DataValue lastValue;

    public AbstractDataQuantizer(BucketCalculator bucketCalculator, DataValue startValue) {
        periodFrom = bucketCalculator.getStartTime();
        periodTo = bucketCalculator.getNextPeriodTo();

        startTime = periodFrom.getMillis();
        this.bucketCalculator = bucketCalculator;

        lastValue = startValue;
        openPeriod(periodFrom, periodTo, lastValue);
    }

    public void data(List<IValueTime> data) {
        for (IValueTime vt : data)
            data(vt.getValue(), vt.getTime());
        done();
    }

    public void data(IValueTime vt) {
        data(vt.getValue(), vt.getTime());
    }

    public void data(DataValue value, long time) {
        if (time < startTime)
            throw new IllegalArgumentException("Data is before start time");

        if (time >= bucketCalculator.getEndTime().getMillis())
            throw new IllegalArgumentException("Data is after end time");

        while (time >= periodTo.getMillis())
            nextPeriod(value, time);

        dataInPeriod(value, time);

        lastValue = value;
    }

    /**
     * Called when no further data will be added to the Quantizer
     */
    public void done() {
        while (periodTo.isBefore(bucketCalculator.getEndTime()))
            nextPeriod(lastValue, periodTo.getMillis());
        closePeriod();
    }

    /**
     * @param endValue
     *            the value that occurs next after the end of this period.
     * @param time 
     * 			  the time that the endValue occurred        
     */
    private void nextPeriod(DataValue endValue, long time) {
        closePeriod();
        periodFrom = periodTo;
        periodTo = bucketCalculator.getNextPeriodTo();
        openPeriod(periodFrom, periodTo, periodFrom.getMillis() == time ? endValue : lastValue);
    }

    /**
     * Tells the quantizer to open the period.
     * 
     * @param startValue
     *            the value that was current at the start of the period, i.e. the latest value that occurred
     *            before the period started. Can be null if the inception of the point occurred after or during this
     *            period. Will no be null even if the value occurred long before the start of the period.
     * @param start
     *            the start time (inclusive) of the period
     * @param end
     *            the end time (exclusive) of the period
     */
    abstract protected void openPeriod(DateTime start, DateTime end, DataValue startValue);

    /**
     * A value that occurred in the period. Data will be provided to this method in chronological order.
     * 
     * @param value
     * @param time
     */
    abstract protected void dataInPeriod(DataValue value, long time);

    /**
     * Tells the quantizer that there is no more data for the period.
     * 
     * @param done
     *            indicates that there will never be any more data in this period
     */
    abstract protected void closePeriod();
}
