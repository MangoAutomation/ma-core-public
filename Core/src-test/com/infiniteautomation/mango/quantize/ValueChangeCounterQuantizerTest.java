/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.quantize;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.statistics.ValueChangeCounter;
import com.infiniteautomation.mango.util.datetime.NextTimePeriodAdjuster;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;

/**
 *
 * @author Terry Packer
 */
public class ValueChangeCounterQuantizerTest extends BaseQuantizerTest{
    @Test
    public void testNoData() throws IOException {
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);

        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

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

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testNoStartValueOneValuePerPeriod() throws IOException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        int value = 1;
        while(time.toInstant().isBefore(to.toInstant())) {
            data.add(new IdPointValueTime(1, new MultistateValue(value), time.toInstant().toEpochMilli()));
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                ZonedDateTime sampleTime = time.plusHours(12);

                //Test first
                Assert.assertEquals(1, stats.getFirstValue().getIntegerValue());
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(1, stats.getLastValue().getIntegerValue());
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                if(counter.getValue() == 1)
                    Assert.assertEquals(null, stats.getStartValue());
                else
                    Assert.assertEquals(1, stats.getStartValue().getIntegerValue());
                //Test count
                Assert.assertEquals(1, stats.getCount());

                //Test Changes
                if(counter.getValue() == 1)
                    Assert.assertEquals(1, stats.getChanges());
                else
                    Assert.assertEquals(0, stats.getChanges());
                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(null, 0, true);
        for(int count = 0; count < data.size(); count++)
            quantizer.row(data.get(count), count + 1);
        quantizer.lastValue(data.get(data.size() - 1), data.size() + 1, true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueNoPeriodValues() throws IOException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                //Test first
                Assert.assertEquals(null, stats.getFirstValue());
                Assert.assertEquals(null, stats.getFirstTime());
                //Test last
                Assert.assertEquals(null, stats.getLastValue());
                Assert.assertEquals(null, stats.getLastTime());
                //Test start
                Assert.assertEquals(1, stats.getStartValue().getIntegerValue());
                //Test count
                Assert.assertEquals(0, stats.getCount());

                //Test Changes
                Assert.assertEquals(0, stats.getChanges());

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new MultistateValue(1), time.minusHours(3).toInstant().toEpochMilli()), 0, true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueAtPeriodStartNoPeriodValues() throws IOException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());
                if(counter.getValue() == 1) {
                    //Test Count for 1st value at period start
                    Assert.assertEquals(1, stats.getCount());
                }else {
                    //No values in any other period
                    Assert.assertEquals(0, stats.getCount());
                }
                //No changes in any period
                Assert.assertEquals(0, stats.getChanges());
                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new MultistateValue(1), time.toInstant().toEpochMilli()), 0, false);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueOneValuePerPeriod() throws IOException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        int value = 1;
        while(time.toInstant().isBefore(to.toInstant())) {
            data.add(new IdPointValueTime(1, new MultistateValue(value), time.toInstant().toEpochMilli()));
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                ZonedDateTime sampleTime = time.plusHours(12);

                //Test first
                Assert.assertEquals(1, stats.getFirstValue().getIntegerValue());
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(1, stats.getLastValue().getIntegerValue());
                Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                Assert.assertEquals(1, stats.getStartValue().getIntegerValue());
                //Test count
                Assert.assertEquals(1, stats.getCount());
                //Test Changes
                Assert.assertEquals(0, stats.getChanges());

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new MultistateValue(1), time.minusHours(3).toInstant().toEpochMilli()), 0, true);
        for(int count = 0; count < data.size(); count++)
            quantizer.row(data.get(count), count + 1);
        quantizer.lastValue(data.get(data.size() - 1), data.size() + 1, true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testNoStartValueManyValuesPerPeriod() throws IOException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        NextTimePeriodAdjuster hourlyAdjuster = new NextTimePeriodAdjuster(TimePeriods.HOURS, 1);

        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        while(time.toInstant().isBefore(to.toInstant())) {
            //Insert 10 values per day
            int value = 1;
            ZonedDateTime daily = ZonedDateTime.ofInstant(time.toInstant(), zoneId);
            for(int i=0; i<10; i++) {
                data.add(new IdPointValueTime(1, new MultistateValue(value), daily.toInstant().toEpochMilli()));
                daily = (ZonedDateTime)hourlyAdjuster.adjustInto(daily);
                value = value + 1;
            }
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                //Test first
                Assert.assertEquals(1, stats.getFirstValue().getIntegerValue());
                Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(10, stats.getLastValue().getIntegerValue());
                Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                if(counter.getValue() == 1)
                    Assert.assertEquals(null, stats.getStartValue());
                else
                    Assert.assertEquals(10, stats.getStartValue().getIntegerValue());
                //Test count
                Assert.assertEquals(10, stats.getCount());
                //Ensure data
                //Test Changes
                Assert.assertEquals(10, stats.getChanges());

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(null, 0, true);
        for(int count = 0; count < data.size(); count++)
            quantizer.row(data.get(count), count + 1);
        quantizer.lastValue(data.get(data.size() - 1), data.size() + 1, true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueManyValuesPerPeriod() throws IOException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        NextTimePeriodAdjuster hourlyAdjuster = new NextTimePeriodAdjuster(TimePeriods.HOURS, 1);

        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        while(time.toInstant().isBefore(to.toInstant())) {
            //Insert 10 values per day
            int value = 1;
            ZonedDateTime daily = ZonedDateTime.ofInstant(time.toInstant(), zoneId);
            for(int i=0; i<10; i++) {
                data.add(new IdPointValueTime(1, new MultistateValue(value), daily.toInstant().toEpochMilli()));
                daily = (ZonedDateTime)hourlyAdjuster.adjustInto(daily);
                value = value + 1;
            }
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                //Test first
                Assert.assertEquals(1, stats.getFirstValue().getIntegerValue());
                Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getFirstTime());
                //Test last
                Assert.assertEquals(10, stats.getLastValue().getIntegerValue());
                Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                if(counter.getValue() == 1)
                    Assert.assertEquals(1, stats.getStartValue().getIntegerValue());
                else
                    Assert.assertEquals(10, stats.getStartValue().getIntegerValue());
                //Test count
                Assert.assertEquals(10, stats.getCount());
                //Ensure data
                if(counter.getValue() == 1) {
                    //Test Changes
                    Assert.assertEquals(9, stats.getChanges());
                }else {
                    //Test Changes
                    Assert.assertEquals(10, stats.getChanges());
                }

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new MultistateValue(1), time.minusHours(3).toInstant().toEpochMilli()), 0, true);
        for(int count = 0; count < data.size(); count++)
            quantizer.row(data.get(count), count + 1);
        quantizer.lastValue(data.get(data.size() - 1), data.size() + 1, true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }

    @Test
    public void testStartValueAtStartManyValuesPerPeriod() throws IOException {
        //Generate data at 12 noon for every day in the period
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        NextTimePeriodAdjuster hourlyAdjuster = new NextTimePeriodAdjuster(TimePeriods.HOURS, 1);

        time = ZonedDateTime.of(2017, 01, 01, 12, 00, 00, 0, zoneId);
        List<IdPointValueTime> data = new ArrayList<>();
        while(time.toInstant().isBefore(to.toInstant())) {
            //Insert 10 values per day
            int value = 1;
            ZonedDateTime daily = ZonedDateTime.ofInstant(time.toInstant(), zoneId);
            for(int i=0; i<10; i++) {
                data.add(new IdPointValueTime(1, new MultistateValue(value), daily.toInstant().toEpochMilli()));
                daily = (ZonedDateTime)hourlyAdjuster.adjustInto(daily);
                value = value + 1;
            }
            time = (ZonedDateTime) adjuster.adjustInto(time);
        }

        //Reset time to track periods
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        ValueChangeCounterQuantizer quantizer = new ValueChangeCounterQuantizer(bc, new StatisticsGeneratorQuantizerCallback<ValueChangeCounter>() {

            @Override
            public void quantizedStatistics(ValueChangeCounter statisticsGenerator) throws IOException {
                counter.increment();
                ValueChangeCounter stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periiodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());

                if(counter.getValue() == 1) {
                    //Test first
                    Assert.assertEquals(1, stats.getFirstValue().getIntegerValue());
                    Assert.assertEquals(time.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                }else {
                    //Test first
                    Assert.assertEquals(1, stats.getFirstValue().getIntegerValue());
                    Assert.assertEquals(time.plusHours(12).toInstant().toEpochMilli(), (long)stats.getFirstTime());
                }
                //Test last
                Assert.assertEquals(10, stats.getLastValue().getIntegerValue());
                Assert.assertEquals(time.plusHours(12).plusHours(9).toInstant().toEpochMilli(), (long)stats.getLastTime());
                //Test start (the first start value will be null
                if(counter.getValue() == 1) {
                    Assert.assertEquals(1, stats.getStartValue().getIntegerValue());
                    //Test count
                    Assert.assertEquals(11, stats.getCount());
                    //Test changes
                    Assert.assertEquals(9, stats.getChanges());
                }else {
                    Assert.assertEquals(10, stats.getStartValue().getIntegerValue());
                    //Test count
                    Assert.assertEquals(10, stats.getCount());
                    //Test changes
                    Assert.assertEquals(10, stats.getChanges());
                }

                //Move to next period time
                time = (ZonedDateTime)adjuster.adjustInto(time);
            }
        });

        quantizer.firstValue(new IdPointValueTime(1, new MultistateValue(1), time.toInstant().toEpochMilli()), 0, false);
        for(int count = 0; count < data.size(); count++)
            quantizer.row(data.get(count), count + 1);
        quantizer.lastValue(data.get(data.size() - 1), data.size() + 1, true);
        quantizer.done();
        Assert.assertEquals(Integer.valueOf(31), counter.getValue());
    }



}
