/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.pointvalue.generator.ConstantPointValueGenerator;
import com.infiniteautomation.mango.pointvalue.generator.PointValueGenerator;
import com.infiniteautomation.mango.quantize.AnalogStatisticsQuantizer;
import com.infiniteautomation.mango.quantize.BucketCalculator;
import com.infiniteautomation.mango.quantize.TemporalAmountBucketCalculator;
import com.serotonin.m2m2.db.dao.BatchPointValue;
import com.serotonin.m2m2.db.dao.pointvalue.NumericAggregate;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
public class StatisticsAggregatorTest {

    final ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC);
    final ZonedDateTime to = from.plusDays(1L);
    final Duration pollPeriod = Duration.ofSeconds(5L);
    final Duration aggregatePeriod = Duration.ofMinutes(15L);
    final long expectedAggregateValues = Duration.between(from, to).dividedBy(aggregatePeriod);

    @Test
    public void emptyStream() {
        BucketCalculator bucketCalc = new TemporalAmountBucketCalculator(from, to, aggregatePeriod);
        List<NumericAggregate> aggregates = StatisticsAggregator.aggregate(Stream.empty(),
                new AnalogStatisticsQuantizer(bucketCalc)).collect(Collectors.toList());

        Assert.assertEquals(expectedAggregateValues, aggregates.size());
        for (var aggregate : aggregates) {
            assertEquals(0L, aggregate.getCount());
            assertEquals(Double.NaN, aggregate.getArithmeticMean(), 0.0D);
            assertEquals(Double.NaN, aggregate.getAverage(), 0.0D);
        }
    }

    @Test
    public void initialValueOnly() {
        PointValueTime initialValue = new PointValueTime(1.0D, from.minusHours(1L).toInstant().toEpochMilli());

        BucketCalculator bucketCalc = new TemporalAmountBucketCalculator(from, to, aggregatePeriod);
        List<NumericAggregate> aggregates = StatisticsAggregator.aggregate(Stream.of(initialValue),
                new AnalogStatisticsQuantizer(bucketCalc)).collect(Collectors.toList());

        Assert.assertEquals(expectedAggregateValues, aggregates.size());
        for (var aggregate : aggregates) {
            assertEquals(0L, aggregate.getCount());
            assertEquals(Double.NaN, aggregate.getArithmeticMean(), 0.0D);
            assertEquals(1.0D, aggregate.getAverage(), 0.0D);
        }
    }

    @Test
    public void aggregate() {
        PointValueGenerator generator = new ConstantPointValueGenerator(from.toInstant(), to.toInstant(), pollPeriod,
                new NumericValue(0.0D));
        var stream = generator.apply(new DataPointVO()).map(BatchPointValue::getValue);

        BucketCalculator bucketCalc = new TemporalAmountBucketCalculator(from, to, aggregatePeriod);
        List<NumericAggregate> aggregates = StatisticsAggregator.aggregate(stream,
                new AnalogStatisticsQuantizer(bucketCalc)).collect(Collectors.toList());
        Assert.assertEquals(expectedAggregateValues, aggregates.size());

        for (var aggregate : aggregates) {
            assertEquals(180L, aggregate.getCount());
            assertEquals(0.0D, aggregate.getArithmeticMean(), 0.0D);
            assertEquals(0.0D, aggregate.getArithmeticMean(), 0.0D);
        }
    }

    @Test
    public void aggregateWithInitialValue() {
        PointValueTime initialValue = new PointValueTime(1.0D, from.minusHours(1L).toInstant().toEpochMilli());
        PointValueGenerator generator = new ConstantPointValueGenerator(from.toInstant(), to.toInstant(), pollPeriod,
                new NumericValue(0.0D));
        var stream = generator.apply(new DataPointVO()).map(BatchPointValue::getValue);

        BucketCalculator bucketCalc = new TemporalAmountBucketCalculator(from, to, aggregatePeriod);
        List<NumericAggregate> aggregates = StatisticsAggregator.aggregate(Stream.concat(Stream.of(initialValue), stream),
                new AnalogStatisticsQuantizer(bucketCalc)).collect(Collectors.toList());
        Assert.assertEquals(expectedAggregateValues, aggregates.size());

        for (var aggregate : aggregates) {
            assertEquals(180L, aggregate.getCount());
            assertEquals(0.0D, aggregate.getArithmeticMean(), 0.0D);
            assertEquals(0.0D, aggregate.getArithmeticMean(), 0.0D);
        }
    }

}