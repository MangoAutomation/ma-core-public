/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.QueryCancelledException;
import com.infiniteautomation.mango.statistics.AnalogStatistics;
import com.infiniteautomation.mango.util.datetime.NextTimePeriodAdjuster;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;


/**
 * Test statistics for the Month of Jan 2017
 *
 * @author Terry Packer
 */
public class AnalogStatisticsQuantizerTest extends BaseQuantizerTest{


    @Test
    public void testNoData() throws QueryCancelledException {
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);

        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());
                //Test Minimum
                Assert.assertEquals(Double.NaN, stats.getMinimumValue(), 0.0001);
                Assert.assertEquals(null, stats.getMinimumTime());
                //Test Maximum
                Assert.assertEquals(Double.NaN, stats.getMaximumValue(), 0.0001);
                Assert.assertEquals(null, stats.getMaximumTime());
                //Test Average
                Assert.assertEquals(Double.NaN, stats.getAverage(), 0.0001);
                //Test Integral
                Assert.assertEquals(Double.NaN, stats.getIntegral(), 0.0001);
                //Test sum
                Assert.assertEquals(0.0d, stats.getSum(), 0.0001);
                //Test first
                Assert.assertEquals(null, stats.getFirstValue());
                Assert.assertEquals(null, stats.getFirstTime());
                //Test last
                Assert.assertEquals(null, stats.getLastValue());
                Assert.assertEquals(null, stats.getLastTime());
                //Test start
                Assert.assertEquals(null, stats.getStartValue());
                //Test count
                Assert.assertEquals(0, stats.getCount());
                //Test delta
                Assert.assertEquals(Double.NaN, stats.getDelta(), 0.0001);

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testNoStartValueOneValuePerPeriod() throws QueryCancelledException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        double value = 1.0;
        while(time.toInstant().isBefore(to.toInstant())) {
            data.add(new IdPointValueTime(1, new NumericValue(value), time.toInstant().toEpochMilli()));
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;

                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                ZonedDateTime sampleTime = time.plusHours(12);
                //Test Minimum
                Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                //Test Maximum
                Assert.assertEquals(1.0, stats.getMaximumValue(), 0.0001);

                if(counter.getValue() == 1) {
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                }else {
                    //Period start if there was a start value
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                }

                //Test Average
                Assert.assertEquals(1.0d, stats.getAverage(), 0.0001);
                //Test Integral
                if(counter.getValue() == 1) {
                    double integral = 1.0d * 12 * 60 * 60;
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }else {
                    //24Hrs
                    double integral = 1.0d * 24 * 60 * 60;
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }
                //Test sum
                Assert.assertEquals(1.0d, stats.getSum(), 0.0001);
                //Test first
                Assert.assertEquals(1.0d, stats.getFirstValue(), 0.0001);
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(1.0d, stats.getLastValue(), 0.0001);
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                if(counter.getValue() == 1)
                    Assert.assertEquals(null, stats.getStartValue());
                else
                    Assert.assertEquals(1.0, stats.getStartValue(), 0.0001);
                //Test count
                Assert.assertEquals(1, stats.getCount());
                //Test delta
                Assert.assertEquals(0.0, stats.getDelta(), 0.0001);

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(null, true);
        for(int count = 0; count < data.size(); count++)
            quantizer.accept(data.get(count));
        quantizer.lastValue(data.get(data.size() - 1), true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueNoPeriodValues() throws QueryCancelledException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                //Test Minimum
                Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                //Test Maximum
                Assert.assertEquals(1.0, stats.getMaximumValue(), 0.0001);
                Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                //Test Average
                Assert.assertEquals(1.0, stats.getAverage(), 0.0001);
                //Test Integral
                double integral = 1.0 * 24*60*60;
                Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                //Test sum
                Assert.assertEquals(0.0d, stats.getSum(), 0.0001);
                //Test first
                Assert.assertEquals(null, stats.getFirstValue());
                Assert.assertEquals(null, stats.getFirstTime());
                //Test last
                Assert.assertEquals(null, stats.getLastValue());
                Assert.assertEquals(null, stats.getLastTime());
                //Test start
                Assert.assertEquals(1.0, stats.getStartValue(), 0.0001);
                //Test count
                Assert.assertEquals(0, stats.getCount());
                //Test delta
                Assert.assertEquals(0.0d, stats.getDelta(), 0.0001);

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new NumericValue(1.0), time.minusHours(3).toInstant().toEpochMilli()), true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueAtPeriodStartNoPeriodValues() throws QueryCancelledException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());
                ZonedDateTime sampleTime = time;
                if(counter.getValue() == 1) {
                    //Test Minimum
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(1.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                    //Test Average
                    Assert.assertEquals(1.0d, stats.getAverage(), 0.0001);
                    //Test Integral
                    //24Hrs
                    double integral = 1.0d * 24 * 60 * 60;
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                    //Test sum
                    Assert.assertEquals(1.0d, stats.getSum(), 0.0001);
                    //Test first
                    Assert.assertEquals(1.0d, stats.getFirstValue(), 0.0001);
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                    //Test last
                    Assert.assertEquals(1.0d, stats.getLastValue(), 0.0001);
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getLastTime());
                    //Test start
                    Assert.assertEquals(1.0, stats.getStartValue(), 0.0001);
                    //Test count
                    Assert.assertEquals(1, stats.getCount());
                    //Test delta
                    Assert.assertEquals(0.0, stats.getDelta(), 0.0001);
                }else {
                    //No data in other periods
                    //Test Minimum
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(1.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                    //Test Average
                    Assert.assertEquals(1.0, stats.getAverage(), 0.0001);
                    //Test Integral
                    double integral = 1.0 * 24*60*60;
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                    //Test sum
                    Assert.assertEquals(0.0d, stats.getSum(), 0.0001);
                    //Test first
                    Assert.assertEquals(null, stats.getFirstValue());
                    Assert.assertEquals(null, stats.getFirstTime());
                    //Test last
                    Assert.assertEquals(null, stats.getLastValue());
                    Assert.assertEquals(null, stats.getLastTime());
                    //Test start
                    Assert.assertEquals(1.0, stats.getStartValue(), 0.0001);
                    //Test count
                    Assert.assertEquals(0, stats.getCount());
                    //Test delta
                    Assert.assertEquals(0.0d, stats.getDelta(), 0.0001);
                }

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new NumericValue(1.0), time.toInstant().toEpochMilli()), false);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    //Test with Start Value and Values
    @Test
    public void testStartValueOneValuePerPeriod() throws QueryCancelledException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        double value = 1.0;
        while(time.toInstant().isBefore(to.toInstant())) {
            data.add(new IdPointValueTime(1, new NumericValue(value), time.toInstant().toEpochMilli()));
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                ZonedDateTime sampleTime = time.plusHours(12);
                //Start Value was 3 hrs before 1st period start
                //Test Minimum
                Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                //Test Maximum
                Assert.assertEquals(1.0, stats.getMaximumValue(), 0.0001);
                Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMaximumTime());

                //Test Average
                Assert.assertEquals(1.0d, stats.getAverage(), 0.0001);
                //Test Integral
                //24Hrs
                double integral = 1.0d * 24 * 60 * 60;
                Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                //Test sum
                Assert.assertEquals(1.0d, stats.getSum(), 0.0001);
                //Test first
                Assert.assertEquals(1.0d, stats.getFirstValue(), 0.0001);
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(1.0d, stats.getLastValue(), 0.0001);
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                Assert.assertEquals(1.0, stats.getStartValue(), 0.0001);
                //Test count
                Assert.assertEquals(1, stats.getCount());
                //Test delta
                Assert.assertEquals(0.0, stats.getDelta(), 0.0001);

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new NumericValue(1.0), time.minusHours(3).toInstant().toEpochMilli()), true);
        for(int count = 0; count < data.size(); count++)
            quantizer.accept(data.get(count));
        quantizer.lastValue(data.get(data.size() - 1), true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }
    //TODO Test with End Value on edge of period end i.e. not a bookend (this won't happen via a query)

    //
    //Many Values Per Period Tests
    //

    @Test
    public void testNoStartValueManyValuesPerPeriod() throws QueryCancelledException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        NextTimePeriodAdjuster hourlyAdjuster = new NextTimePeriodAdjuster(TimePeriods.HOURS, 1);

        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        while(time.toInstant().isBefore(to.toInstant())) {
            //Insert 10 values per day
            double value = 1.0;
            ZonedDateTime daily = ZonedDateTime.ofInstant(time.toInstant(), zoneId);
            for(int i=0; i<10; i++) {
                data.add(new IdPointValueTime(1, new NumericValue(value), daily.toInstant().toEpochMilli()));
                daily = (ZonedDateTime)hourlyAdjuster.adjustInto(daily);
                value = value + 1.0d;
            }
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                if(counter.getValue() == 1) {
                    //Test Minimum
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(10.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                }else {
                    //Have start value
                    //Test Minimum
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(10.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                }
                //Test Average
                //1-9 for 1hr each, 10 for 12hrs at the start and 2hrs at the end
                if(counter.getValue() == 1) {
                    double integral = 1d*60d*60d + 2d*60*60 + 3d*60*60 + 4d*60*60 + 5d*60*60 + 6d*60*60 + 7d*60*60 + 8d*60*60 + 9d*60*60;
                    integral = integral + 10d * 3d*60*60;
                    double average = integral / (12d*60d*60d); //first 12hrs didn't have a value
                    Assert.assertEquals(average, stats.getAverage(), 0.0001);
                    //Test Integral
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }else {
                    double integral = 1d*60d*60d + 2d*60*60 + 3d*60*60 + 4d*60*60 + 5d*60*60 + 6d*60*60 + 7d*60*60 + 8d*60*60 + 9d*60*60;
                    integral = integral + 10d * 15d*60*60;
                    double average = integral / (24d*60d*60d);
                    Assert.assertEquals(average, stats.getAverage(), 0.0001);
                    //Test Integral
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }


                //Test sum
                Assert.assertEquals(55d, stats.getSum(), 0.0001);
                //Test first
                Assert.assertEquals(1.0d, stats.getFirstValue(), 0.0001);
                Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(10.0d, stats.getLastValue(), 0.0001);
                Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                if(counter.getValue() == 1)
                    Assert.assertEquals(null, stats.getStartValue());
                else
                    Assert.assertEquals(10.0, stats.getStartValue(), 0.0001);
                //Test count
                Assert.assertEquals(10, stats.getCount());
                //Test delta
                if(counter.getValue() == 1) {
                    //1 to 10
                    Assert.assertEquals(9.0, stats.getDelta(), 0.0001);
                }else {
                    Assert.assertEquals(0.0, stats.getDelta(), 0.0001);
                }

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(null, true);
        for(int count = 0; count < data.size(); count++)
            quantizer.accept(data.get(count));
        quantizer.lastValue(data.get(data.size() - 1), true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueManyValuesPerPeriod() throws QueryCancelledException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        NextTimePeriodAdjuster hourlyAdjuster = new NextTimePeriodAdjuster(TimePeriods.HOURS, 1);

        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        while(time.toInstant().isBefore(to.toInstant())) {
            //Insert 10 values per day
            double value = 1.0;
            ZonedDateTime daily = ZonedDateTime.ofInstant(time.toInstant(), zoneId);
            for(int i=0; i<10; i++) {
                data.add(new IdPointValueTime(1, new NumericValue(value), daily.toInstant().toEpochMilli()));
                daily = (ZonedDateTime)hourlyAdjuster.adjustInto(daily);
                value = value + 1.0d;
            }
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                if(counter.getValue() == 1) {
                    //Test Minimum
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(10.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                }else {
                    //Test Minimum
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(10.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                }

                //Test Average
                //1-9 for 1hr each, 10 for 12hrs at the start and 2hrs at the end
                if(counter.getValue() == 1) {
                    double integral = 1d*13*60d*60d + 2d*60*60 + 3d*60*60 + 4d*60*60 + 5d*60*60 + 6d*60*60 + 7d*60*60 + 8d*60*60 + 9d*60*60;
                    integral = integral + 10d * 3d*60*60;
                    double average = integral / (24d*60d*60d);
                    Assert.assertEquals(average, stats.getAverage(), 0.0001);
                    //Test Integral
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }else {
                    double integral = 1d*60d*60d + 2d*60*60 + 3d*60*60 + 4d*60*60 + 5d*60*60 + 6d*60*60 + 7d*60*60 + 8d*60*60 + 9d*60*60;
                    integral = integral + 10d * 15d*60*60;
                    double average = integral / (24d*60d*60d);
                    Assert.assertEquals(average, stats.getAverage(), 0.0001);
                    //Test Integral
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }

                //Test sum
                Assert.assertEquals(55d, stats.getSum(), 0.0001);
                //Test first
                Assert.assertEquals(1.0d, stats.getFirstValue(), 0.0001);
                Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(10.0d, stats.getLastValue(), 0.0001);
                Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                if(counter.getValue() == 1)
                    Assert.assertEquals(1.0, stats.getStartValue(), 0.0001);
                else
                    Assert.assertEquals(10.0, stats.getStartValue(), 0.0001);
                //Test count
                Assert.assertEquals(10, stats.getCount());
                //Test delta
                if(counter.getValue() == 1) {
                    //1 to 10
                    Assert.assertEquals(9.0, stats.getDelta(), 0.0001);
                }else {
                    Assert.assertEquals(0.0, stats.getDelta(), 0.0001);
                }

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new NumericValue(1.0), time.minusHours(3).toInstant().toEpochMilli()), true);
        for(int count = 0; count < data.size(); count++)
            quantizer.accept(data.get(count));
        quantizer.lastValue(data.get(data.size() - 1), true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueAtStartManyValuesPerPeriod() throws QueryCancelledException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        NextTimePeriodAdjuster hourlyAdjuster = new NextTimePeriodAdjuster(TimePeriods.HOURS, 1);

        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        while(time.toInstant().isBefore(to.toInstant())) {
            //Insert 10 values per day
            double value = 1.0;
            ZonedDateTime daily = ZonedDateTime.ofInstant(time.toInstant(), zoneId);
            for(int i=0; i<10; i++) {
                data.add(new IdPointValueTime(1, new NumericValue(value), daily.toInstant().toEpochMilli()));
                daily = (ZonedDateTime)hourlyAdjuster.adjustInto(daily);
                value = value + 1.0d;
            }
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        AnalogStatisticsQuantizer quantizer = new AnalogStatisticsQuantizer(bc, new StatisticsGeneratorQuantizerCallback<AnalogStatistics>() {

            @Override
            public void quantizedStatistics(AnalogStatistics statisticsGenerator) throws QueryCancelledException {
                counter.increment();
                AnalogStatistics stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                //Test Minimum
                if(counter.getValue() == 1) {
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(10.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getMaximumTime());

                }else {
                    Assert.assertEquals(1.0, stats.getMinimumValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getMinimumTime());
                    //Test Maximum
                    Assert.assertEquals(10.0, stats.getMaximumValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getMaximumTime());
                }

                //Test Average
                //1-9 for 1hr each, 10 for 12hrs at the start and 2hrs at the end
                if(counter.getValue() == 1) {
                    double integral = 1d*13*60d*60d + 2d*60*60 + 3d*60*60 + 4d*60*60 + 5d*60*60 + 6d*60*60 + 7d*60*60 + 8d*60*60 + 9d*60*60;
                    integral = integral + 10d * 3d*60*60;
                    double average = integral / (24d*60d*60d);
                    Assert.assertEquals(average, stats.getAverage(), 0.0001);
                    //Test Integral
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }else {
                    double integral = 1d*60d*60d + 2d*60*60 + 3d*60*60 + 4d*60*60 + 5d*60*60 + 6d*60*60 + 7d*60*60 + 8d*60*60 + 9d*60*60;
                    integral = integral + 10d * 15d*60*60;
                    double average = integral / (24d*60d*60d);
                    Assert.assertEquals(average, stats.getAverage(), 0.0001);
                    //Test Integral
                    Assert.assertEquals(integral, stats.getIntegral(), 0.0001);
                }

                //Test sum
                if(counter.getValue() == 1) {
                    Assert.assertEquals(56d, stats.getSum(), 0.0001);
                    //Test first
                    Assert.assertEquals(1.0d, stats.getFirstValue(), 0.0001);
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                }else {
                    Assert.assertEquals(55d, stats.getSum(), 0.0001);
                    //Test first
                    Assert.assertEquals(1.0d, stats.getFirstValue(), 0.0001);
                    Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getFirstTime());
                }
                //Test last
                Assert.assertEquals(10.0d, stats.getLastValue(), 0.0001);
                Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getLastTime());

                if(counter.getValue() == 1) {
                    //Test start (the first start value will be null
                    Assert.assertEquals(1.0, stats.getStartValue(), 0.0001);
                    //Test count
                    Assert.assertEquals(11, stats.getCount());
                }else {
                    //Test start (the first start value will be null
                    Assert.assertEquals(10.0, stats.getStartValue(), 0.0001);
                    //Test count
                    Assert.assertEquals(10, stats.getCount());
                }

                //Test delta
                if(counter.getValue() == 1) {
                    //1 to 10
                    Assert.assertEquals(9.0, stats.getDelta(), 0.0001);
                }else {
                    Assert.assertEquals(0.0, stats.getDelta(), 0.0001);
                }

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new NumericValue(1.0), time.toInstant().toEpochMilli()), false);
        for(int count = 0; count < data.size(); count++)
            quantizer.accept(data.get(count));
        quantizer.lastValue(data.get(data.size() - 1), true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }
}
