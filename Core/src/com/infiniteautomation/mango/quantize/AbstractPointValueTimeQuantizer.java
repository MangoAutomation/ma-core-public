/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.io.IOException;
import java.time.Instant;

import com.infiniteautomation.mango.db.query.WideCallback;
import com.infiniteautomation.mango.db.query.QueryCancelledException;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * Combine a statistics callback with a generator to pass into a point value query.
 *
 * The statistics are generated based on the bucket calculator's times bc.startTime <= stats < bc.endTime
 *
 * The general use would be:
 *
 * 1. quantizer.firstValue() *optional
 * 2. quantizer.row()... *optional
 * 3. quantizer.lastValue() *optional
 * 4. quantizer.done() - must call to produce statistics
 *
 * @author Terry Packer
 */
abstract public class AbstractPointValueTimeQuantizer<T extends StatisticsGenerator> implements WideCallback<IdPointValueTime> {

    private final BucketCalculator bucketCalculator;
    private final StatisticsGeneratorQuantizerCallback<T> callback;
    private final long startTime;
    private final long endTime;

    private T statistics;
    private Instant periodFrom;
    private Instant periodTo;
    private long periodToMillis; //For performance
    private IValueTime lastValue;

    public AbstractPointValueTimeQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<T> callback) {
        this.bucketCalculator = bucketCalculator;
        this.callback = callback;

        periodFrom = bucketCalculator.getStartTime().toInstant();
        periodTo = bucketCalculator.getNextPeriodTo().toInstant();
        periodToMillis = periodTo.toEpochMilli();
        startTime = periodFrom.toEpochMilli();
        endTime =  bucketCalculator.getEndTime().toInstant().toEpochMilli();
    }

    @Override
    public void accept(IdPointValueTime vt) {
        long time = vt.getTime();
        if (time < startTime)
            throw new IllegalArgumentException("Data is before start time");

        if (time >= endTime)
            throw new IllegalArgumentException("Data is after end time");

        while (time >= periodToMillis)
            nextPeriod(vt, time);

        dataInPeriod(vt);

        lastValue = vt;
    }

    @Override
    public void firstValue(IdPointValueTime value, boolean bookend) {
        openPeriod(periodFrom, periodTo, value);
        if(!bookend)
            accept(value);
    }

    @Override
    public void lastValue(IdPointValueTime value, boolean bookend) {
        if(!bookend)
            accept(value);
    }

    /**
     * Called when no further data will be added to the Quantizer
     */
    public void done() throws QueryCancelledException {
        //Special case where there is no data
        if(lastValue == null && periodFrom.equals(bucketCalculator.getStartTime().toInstant()))
            openPeriod(periodFrom, periodTo, lastValue);

        Instant endInstant = bucketCalculator.getEndTime().toInstant();
        while (periodTo.isBefore(endInstant))
            nextPeriod(lastValue, periodTo.toEpochMilli());
        closePeriod();
    }

    /**
     * @param endValue
     *            the value that occurs next after the end of this period.
     * @param time
     * 			  the time that the endValue occurred
     */
    private void nextPeriod(IValueTime endValue, long time) throws QueryCancelledException {
        closePeriod();
        periodFrom = periodTo;
        periodTo = bucketCalculator.getNextPeriodTo().toInstant();
        periodToMillis = periodTo.toEpochMilli();
        openPeriod(periodFrom, periodTo, periodFrom.toEpochMilli() == time ? endValue : lastValue);
    }

    /**
     * Fast forward in time to cover any gaps in data with the last value known
     */
    public void fastForward(long time) throws QueryCancelledException {
        while (periodToMillis <= time)
            nextPeriod(lastValue, periodTo.toEpochMilli());
    }

    /**
     * Create a new statistics generator for the period with the start value
     * @param startValue
     *            the value that was current at the start of the period, i.e. the latest value that occurred
     *            before the period started. Can be null if the inception of the point occurred after or during this
     *            period. Will no be null even if the value occurred long before the start of the period.
     * @param start
     *            the start time (inclusive) of the period
     * @param end
     *            the end time (exclusive) of the period
     *
     * @return new statistics object for a period
     */
    abstract protected T createStatistics(Instant start, Instant end, IValueTime startValue);

    /**
     * Tells the quantizer to open the period.
     *
     * @param startValue
     *            the value that was current at the start of the period, i.e. the latest value that occurred
     *            before the period started. Can be null if the inception of the point occurred after or during this
     *            period. Will not be null even if the value occurred long before the start of the period.
     * @param start
     *            the start time (inclusive) of the period
     * @param end
     *            the end time (exclusive) of the period
     */
    protected void openPeriod(Instant start, Instant end, IValueTime startValue) {
        this.lastValue = startValue;
        this.statistics = createStatistics(start, end, startValue);
    }

    /**
     * A value that occurred in the period. Data will be provided to this method in chronological order.
     *
     */
    protected void dataInPeriod(IValueTime vt) {
        statistics.addValueTime(vt);
    }

    /**
     * Tells the quantizer that there is no more data for the period.
     *
     * @param done
     *            indicates that there will never be any more data in this period
     */
    protected void closePeriod() throws QueryCancelledException {
        if (statistics != null) {
            statistics.done();
            callback.quantizedStatistics(statistics);
        }
    }
}
