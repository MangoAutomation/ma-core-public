/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

@State(Scope.Benchmark)
public class DataPointEventsBenchmarks {
    public static final String ID = "id";
    public static final String POINT_EVENT_DETECTOR = "pointEventDetector";
    public static final String DATA_POINT = "dataPoint";
    public static final String LITERAL = "literal";
    public static final String BENCHMARK_DATA_POINT_EVENT_FOR = "Benchmark data point event for  ";
    private MangoBenchmark base;

    private DataSourceService dataSourceService;


    public DataPointEventsBenchmarks() {
        this.base = new MangoBenchmark();
    }

    @Test
    public void runBenchmark() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(this.getClass().getName())
                .build();
        new Runner(opt).run();
    }

    @State(Scope.Benchmark)
    public static class BenchmarkParams {
        //@Param({ "1000", "10000", "100000" }) fails in my computer
        @Param({"50", "100", "200"})
        public int dataPointCount;

        @Param({"1"})
        public int eventsPerDataPoint;

        @Param({"1"})
        public int limit;

    }

    @Benchmark
    @Threads(1)
    @Fork(value = 0, warmups = 0)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void insertSpeed(DataPointEventsBenchmarks.BenchmarkParams params) {
        DataPointService service = Common.getBean(DataPointService.class);
        RunAs runAs = Common.getBean(RunAs.class);
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            List<IDataPoint> points = base.getMango().createMockDataPointsWithDetectors(params.dataPointCount);
            for (IDataPoint point :
                    points) {
                //System.out.println(((DataPointVO) point).getName());
                service.buildQuery().sort(ID, true).query(pointAux -> {
                    List<AbstractPointEventDetectorVO> detectors = EventDetectorDao.getInstance().getWithSource(point.getId(), (DataPointVO) point);
                    if (detectors.size() > 0) {
                        DataPointEventType type = new DataPointEventType((DataPointVO) point, detectors.get(0));
                        HashMap context = new HashMap();
                        context.put(POINT_EVENT_DETECTOR, detectors.get(0));
                        context.put(DATA_POINT, point);

                        AtomicInteger eventCount = new AtomicInteger();
                        for (int i = 0; i < params.eventsPerDataPoint; i++) {

                            Common.eventManager.raiseEvent(type,
                                    Common.timer.currentTimeMillis(),
                                    false, AlarmLevels.INFORMATION,
                                    new TranslatableMessage(LITERAL, BENCHMARK_DATA_POINT_EVENT_FOR + point.getName()),
                                    context);

                            eventCount.getAndIncrement();
                        }
                        //System.out.println("Raised " + params.eventsPerDataPoint +  " events for point  " + point.getName());
                    }
                }, params.limit, 0);
            }
        });
    }

    @BeforeClass
    public static void staticSetup() throws IOException {
        System.out.println("Before class");
        MangoTestBase.staticSetup();
    }

    @Before
    public void before() {
        System.out.println("Before");
        base.before();
        dataSourceService = Common.getBean(DataSourceService.class);
    }

    @After
    public void after() {
        System.out.println("After");
        base.after();
    }

    @AfterClass
    public static void staticTearDown() throws IOException {
        System.out.println("After class");
        MangoTestBase.staticTearDown();
    }
}
