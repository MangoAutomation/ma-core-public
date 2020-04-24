/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.quantize;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.statistics.StartsAndRuntime;
import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.infiniteautomation.mango.util.datetime.NextTimePeriodAdjuster;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;

/**
 *
 * @author Terry Packer
 */
public class StartsAndRuntimeListQuantizerTest extends BaseQuantizerTest{


    @Test
    public void testNoData() throws IOException {
        NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(TimePeriods.DAYS, 1);
        time = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);

        MutableInt counter = new MutableInt(0);
        BucketCalculator bc = new TimePeriodBucketCalculator(from, to, TimePeriods.DAYS, 1);
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
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
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
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

                //Test StartsList
                Map<Object, StartsAndRuntime> map = stats.getStartsAndRuntime();
                StartsAndRuntime one = map.get(1);
                Assert.assertEquals(1, one.getStarts());
                Assert.assertEquals(100.0d, one.getPercentage(), 0.0001);
                Assert.assertEquals(1.0d, one.getProportion(), 0.0001);
                if(counter.getValue() == 1)
                    Assert.assertEquals(12*60*60*1000, one.getRuntime());
                else
                    Assert.assertEquals(24*60*60*1000, one.getRuntime());

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
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
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
                //Ensure data
                Assert.assertEquals(1, stats.getStartsAndRuntime().size());
                StartsAndRuntime one = stats.getStartsAndRuntime().get(1);
                Assert.assertEquals(0, one.getStarts());
                Assert.assertEquals(100.0d, one.getPercentage(), 0.0001);
                Assert.assertEquals(1.0d, one.getProportion(), 0.0001);
                Assert.assertEquals(24*60*60*1000, one.getRuntime());

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
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
                //Test periodStart
                Assert.assertEquals(time.toInstant().toEpochMilli(), stats.getPeriodStartTime());
                //Test periodEnd
                Assert.assertEquals(time.plusDays(1).toInstant().toEpochMilli(), stats.getPeriodEndTime());
                ZonedDateTime sampleTime = time;
                if(counter.getValue() == 1) {
                    //Test first
                    Assert.assertEquals(1, stats.getFirstValue().getIntegerValue());
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getFirstTime());
                    //Test last
                    Assert.assertEquals(1, stats.getLastValue().getIntegerValue());
                    Assert.assertEquals(sampleTime.toInstant().toEpochMilli(), (long)stats.getLastTime());
                    //Test start
                    Assert.assertEquals(1, stats.getStartValue().getIntegerValue());
                    //Test count
                    Assert.assertEquals(1, stats.getCount());
                    //Ensure data
                    Assert.assertEquals(1, stats.getStartsAndRuntime().size());
                    StartsAndRuntime one = stats.getStartsAndRuntime().get(1);
                    Assert.assertEquals(1, one.getStarts());
                    Assert.assertEquals(100.0d, one.getPercentage(), 0.0001);
                    Assert.assertEquals(1.0d, one.getProportion(), 0.0001);
                    Assert.assertEquals(24*60*60*1000, one.getRuntime());
                }else {
                    //No data in other periods
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
                    //Ensure data
                    Assert.assertEquals(1, stats.getStartsAndRuntime().size());
                    StartsAndRuntime one = stats.getStartsAndRuntime().get(1);
                    Assert.assertEquals(0, one.getStarts());
                    Assert.assertEquals(100.0d, one.getPercentage(), 0.0001);
                    Assert.assertEquals(1.0d, one.getProportion(), 0.0001);
                    Assert.assertEquals(24*60*60*1000, one.getRuntime());
                }

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
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
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
                //Ensure data
                Assert.assertEquals(1, stats.getStartsAndRuntime().size());
                StartsAndRuntime one = stats.getStartsAndRuntime().get(1);
                Assert.assertEquals(1, one.getStarts());
                Assert.assertEquals(100.0d, one.getPercentage(), 0.0001);
                Assert.assertEquals(1.0d, one.getProportion(), 0.0001);
                Assert.assertEquals(24*60*60*1000, one.getRuntime());

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
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
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
                if(counter.getValue() == 1) {
                    //1 to 10, was 10 for last 3 hrs of day
                    Assert.assertEquals(10, stats.getStartsAndRuntime().size());
                    for(StartsAndRuntime value : stats.getData()) {
                        switch((Integer)value.getValue()) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(1d/12d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(1d/12d, value.getProportion(), 0.0001);
                                Assert.assertEquals(60*60*1000, value.getRuntime());
                                break;
                            case 10:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(3d/12d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(3d/12d, value.getProportion(), 0.0001);
                                Assert.assertEquals(3*60*60*1000, value.getRuntime());
                                break;
                        }
                    }

                }else {
                    //Start in state 10 for 12 hrs more
                    Assert.assertEquals(10, stats.getStartsAndRuntime().size());
                    for(StartsAndRuntime value : stats.getData()) {
                        switch((Integer)value.getValue()) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(1d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(1d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(60*60*1000, value.getRuntime());
                                break;
                            case 10:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(15d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(15d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(15*60*60*1000, value.getRuntime());
                                break;
                        }
                    }
                }

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
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
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
                    //1 to 10, was 10 for last 3 hrs of day
                    Assert.assertEquals(10, stats.getStartsAndRuntime().size());
                    for(StartsAndRuntime value : stats.getData()) {
                        switch((Integer)value.getValue()) {
                            case 1:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(13d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(13d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(13*60*60*1000, value.getRuntime());
                                break;
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(1d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(1d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(60*60*1000, value.getRuntime());
                                break;
                            case 10:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(3d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(3d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(3*60*60*1000, value.getRuntime());
                                break;
                        }
                    }

                }else {
                    //Start in state 10 for 12 hrs more
                    Assert.assertEquals(10, stats.getStartsAndRuntime().size());
                    for(StartsAndRuntime value : stats.getData()) {
                        switch((Integer)value.getValue()) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(1d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(1d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(60*60*1000, value.getRuntime());
                                break;
                            case 10:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(15d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(15d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(15*60*60*1000, value.getRuntime());
                                break;
                        }
                    }
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
        StartsAndRuntimeListQuantizer quantizer = new StartsAndRuntimeListQuantizer(bc, new StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList>() {

            @Override
            public void quantizedStatistics(StartsAndRuntimeList statisticsGenerator) throws IOException {
                counter.increment();
                StartsAndRuntimeList stats = statisticsGenerator;
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
                }else {
                    Assert.assertEquals(10, stats.getStartValue().getIntegerValue());
                    //Test count
                    Assert.assertEquals(10, stats.getCount());
                }

                //Ensure data
                if(counter.getValue() == 1) {
                    //1 to 10, was 10 for last 3 hrs of day
                    Assert.assertEquals(10, stats.getStartsAndRuntime().size());
                    for(StartsAndRuntime value : stats.getData()) {
                        switch((Integer)value.getValue()) {
                            case 1:
                                Assert.assertEquals(2, value.getStarts());
                                Assert.assertEquals(100d*(13d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(13d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(13*60*60*1000, value.getRuntime());
                                break;
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(1d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(1d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(60*60*1000, value.getRuntime());
                                break;
                            case 10:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(3d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(3d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(3*60*60*1000, value.getRuntime());
                                break;
                        }
                    }

                }else {
                    //Start in state 10 for 12 hrs more
                    Assert.assertEquals(10, stats.getStartsAndRuntime().size());
                    for(StartsAndRuntime value : stats.getData()) {
                        switch((Integer)value.getValue()) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(1d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(1d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(60*60*1000, value.getRuntime());
                                break;
                            case 10:
                                Assert.assertEquals(1, value.getStarts());
                                Assert.assertEquals(100d*(15d/24d), value.getPercentage(), 0.0001);
                                Assert.assertEquals(15d/24d, value.getProportion(), 0.0001);
                                Assert.assertEquals(15*60*60*1000, value.getRuntime());
                                break;
                        }
                    }
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
