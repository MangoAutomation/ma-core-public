/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.infiniteautomation.mango.pointvalue.generator.LinearPointValueGenerator;
import com.infiniteautomation.mango.pointvalue.generator.PointValueGenerator;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;

/**
 * @author Jared Wiltshire
 */
public class AggregateDaoTest extends MangoTestBase {

    private PointValueDao pointValueDao;

    @Override
    public void before() {
        super.before();

        ApplicationContext context = MangoTestBase.lifecycle.getRuntimeContext();
        this.pointValueDao = context.getBean(PointValueDao.class);
    }

    @Test
    public void query() {
        var ds = createMockDataSource();
        var point = createMockDataPoint(ds, new MockPointLocatorVO(DataType.NUMERIC, false));

        ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC);
        ZonedDateTime to = from.plusDays(1L);
        Duration period = Duration.ofSeconds(5L);
        PointValueGenerator generator = new LinearPointValueGenerator(from.toInstant(), to.toInstant(), period, 0.0D, 1.0D);
        pointValueDao.savePointValues(generator.apply(point));

        Duration aggregatePeriod = Duration.ofMinutes(15L);
        long outputExpectedSamples = Duration.between(from, to).dividedBy(aggregatePeriod);
        AggregateDao aggregateDao = pointValueDao.getAggregateDao(aggregatePeriod);

        try (var stream = aggregateDao.query(point, from, to, null)) {
            var list = stream.collect(Collectors.toList());
            Assert.assertEquals(outputExpectedSamples, list.size());

            for (int i = 0; i < outputExpectedSamples; i++) {
                var destinationValue = list.get(i);
                assertEquals(point.getSeriesId(), destinationValue.getSeriesId());
                assertEquals(from.plus(aggregatePeriod.multipliedBy(i)).toInstant().toEpochMilli(), destinationValue.getTime());
                NumericAggregate aggregate = (NumericAggregate) destinationValue.getValue();

                assertEquals(180, aggregate.getCount());
                double expectedAverage = 180.0 * i + 89.5;
                assertEquals(expectedAverage, aggregate.getArithmeticMean(), 0.0D);
            }
        }
    }

}