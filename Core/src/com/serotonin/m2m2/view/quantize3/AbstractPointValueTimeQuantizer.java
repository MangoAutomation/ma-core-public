/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.view.quantize3;

import java.io.IOException;
import java.time.Instant;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * Combine a statistics callback with a generator to pass into a point value query
 *
 * @author Terry Packer
 */
abstract public class AbstractPointValueTimeQuantizer<T extends StatisticsGenerator> implements BookendQueryCallback<IdPointValueTime>{
    
    private final BucketCalculator bucketCalculator;
    private final StatisticsGeneratorQuantizerCallback<StatisticsGenerator> callback;
    private final long startTime;
    private final long endTime;

    private T statistics;
    private Instant periodFrom;
    private Instant periodTo;
    private long periodToMillis; //For performance
    private DataValue lastValue;

    public AbstractPointValueTimeQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<StatisticsGenerator> callback) {
        this.bucketCalculator = bucketCalculator;
        this.callback = callback;
        
        periodFrom = bucketCalculator.getStartTime().toInstant();
        periodTo = bucketCalculator.getNextPeriodTo().toInstant();
        periodToMillis = periodTo.toEpochMilli();
        startTime = periodFrom.toEpochMilli();
        endTime =  bucketCalculator.getEndTime().toInstant().toEpochMilli();
    }
    
    /*
     * (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.PVTQueryCallback#row(com.serotonin.m2m2.rt.dataImage.PointValueTime, int)
     */
    @Override
    public void row(IdPointValueTime vt, int index) throws IOException{
        long time = vt.getTime();
        if (time < startTime)
            throw new IllegalArgumentException("Data is before start time");

        if (time >= endTime)
            throw new IllegalArgumentException("Data is after end time");

        while (time >= periodToMillis)
            nextPeriod(vt.getValue(), time);

        dataInPeriod(vt);

        lastValue = vt.getValue();
    }
    
    /*
     * (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.BookendQueryCallback#firstValue(com.serotonin.m2m2.rt.dataImage.PointValueTime, int, boolean)
     */
    @Override
    public void firstValue(IdPointValueTime value, int index, boolean bookend) {
        if(!bookend)
            lastValue = value.getValue();
        openPeriod(periodFrom, periodTo, lastValue);
    }

    /*
     * (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.BookendQueryCallback#lastValue(com.serotonin.m2m2.rt.dataImage.PointValueTime, int)
     */
    @Override
    public void lastValue(IdPointValueTime value, int index) throws IOException {
        done();
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.PVTQueryCallback#cancelled(java.io.IOException)
     */
    @Override
    public void cancelled(IOException e) {
        throw new ShouldNeverHappenException(e);
    }
    
    /**
     * Called when no further data will be added to the Quantizer
     * @throws IOException 
     */
    public void done() throws IOException {
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
     * @throws IOException 
     */
    private void nextPeriod(DataValue endValue, long time) throws IOException {
        closePeriod();
        periodFrom = periodTo;
        periodTo = bucketCalculator.getNextPeriodTo().toInstant();
        periodToMillis = periodTo.toEpochMilli();
        openPeriod(periodFrom, periodTo, periodFrom.toEpochMilli() == time ? endValue : lastValue);
    }

    /**
     * Fast forward in time to cover any gaps in data with the last value known
     * @param time
     * @throws IOException 
     */
    public void fastForward(long time) throws IOException {
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
    abstract protected T createStatistics(Instant start, Instant end, DataValue startValue);
    
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
    protected void openPeriod(Instant start, Instant end, DataValue startValue) {
        this.statistics = createStatistics(start, end, startValue);
    }

    /**
     * A value that occurred in the period. Data will be provided to this method in chronological order.
     * 
     * @param value
     * @param time
     */
    protected void dataInPeriod(IValueTime vt) {
        statistics.addValueTime(vt);
    }

    /**
     * Tells the quantizer that there is no more data for the period.
     * 
     * @param done
     *            indicates that there will never be any more data in this period
     * @throws IOException 
     */
    protected void closePeriod() throws IOException {
        if (statistics != null) {
            statistics.done();
            callback.quantizedStatistics(statistics);
        }
    }
}
