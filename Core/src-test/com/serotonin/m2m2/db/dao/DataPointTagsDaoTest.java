/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

public class DataPointTagsDaoTest extends MangoTestBase {

    private final int numPoints = 1000;
    private final int numTags = 10;
    private final Random random = new Random();

    @Before
    @Override
    public void before() {
//        properties.setProperty("db.type", "mysql");
//        properties.setProperty("db.url", "jdbc:mysql:///mango2");
//        properties.setProperty("db.username", "mango");
//        properties.setProperty("db.password", "mango");
        super.before();
    }

//    @Test
    public void updateTags() {
        System.out.println(Common.MA_DATA_PATH);

        LongSummaryStatistics stats;
        DataPointService dataPointService = Common.getBean(DataPointService.class);
        MockDataSourceVO ds = createMockDataSource(true);

        List<DataPointVO> points = Stream.generate(() -> createMockDataPoint(ds,
                new MockPointLocatorVO(DataTypes.NUMERIC, true), true)
        ).limit(numPoints).collect(Collectors.toList());

        AtomicInteger count = new AtomicInteger();
        List<String> tagKeys = Stream.generate(() -> "key" + count.getAndIncrement())
                .limit(numTags).collect(Collectors.toList());

        // add half the tags
        stats = points.stream().mapToLong(point -> {
            point.setTags(tagKeys.stream().limit(numTags / 2).collect(Collectors.toMap(Function.identity(), k -> k + "_value" + random.nextInt(10))));
            long start = System.currentTimeMillis();
            dataPointService.update(point.getId(), point);
            return System.currentTimeMillis() - start;
        }).summaryStatistics();
        System.out.println("add half the tags: " + stats);

        // add/update the tags
        stats = points.stream().mapToLong(point -> {
            point.setTags(tagKeys.stream().collect(Collectors.toMap(Function.identity(), k -> k + "_value" + random.nextInt(10))));
            long start = System.currentTimeMillis();
            dataPointService.update(point.getId(), point);
            return System.currentTimeMillis() - start;
        }).summaryStatistics();
        System.out.println("add/update the tags: " + stats);

        // save without updating tags
        stats = points.stream().mapToLong(point -> {
            long start = System.currentTimeMillis();
            dataPointService.update(point.getId(), point);
            return System.currentTimeMillis() - start;
        }).summaryStatistics();
        System.out.println("save without updating tags: " + stats);

        // remove half of the tags
        stats = points.stream().mapToLong(point -> {
            Map<String, String> existing = point.getTags();
            point.setTags(tagKeys.stream().limit(numTags / 2).collect(Collectors.toMap(Function.identity(), existing::get)));
            long start = System.currentTimeMillis();
            dataPointService.update(point.getId(), point);
            return System.currentTimeMillis() - start;
        }).summaryStatistics();
        System.out.println("remove half of the tags: " + stats);

    }
}
