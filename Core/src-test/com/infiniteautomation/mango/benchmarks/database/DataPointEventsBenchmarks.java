/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.infiniteautomation.mango.benchmarks.MangoBenchmark;
import com.infiniteautomation.mango.benchmarks.MangoBenchmarkParameters;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.EventInstanceService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

@State(Scope.Benchmark)
public class DataPointEventsBenchmarks extends MangoBenchmark {

    public static final String ID = "id";
    public static final String POINT_EVENT_DETECTOR = "pointEventDetector";
    public static final String DATA_POINT = "dataPoint";
    public static final String LITERAL = "literal";
    public static final String BENCHMARK_DATA_POINT_EVENT_FOR = "Benchmark data point event for  ";

    @State(Scope.Benchmark)
    public static class BenchmarkParams extends MangoBenchmarkParameters {

        @Param({"1", "100", "1000"})
        public int dataPoints;

        @Param({"1"})
        public int tagsPerPoint;

        @Param({"1"})
        public int detectorsPerPoint;

        @Param({"1"})
        public int eventsPerEventDetector;

        @Param({"false"})
        public boolean loadReadPermission;

        public EventInstanceService eventInstanceService;
        public Map<String, String> tags = new HashMap<>();
    }

    @Setup(Level.Trial)
    public void setupTrial(BenchmarkParams params) {
        params.mango.setEventManager(new EventManagerImpl());
        super.setupTrial(params);
        params.eventInstanceService = Common.getBean(EventInstanceService.class);
    }

    @Setup(Level.Iteration)
    public void setupIteration(BenchmarkParams params) {
        DataPointService service = Common.getBean(DataPointService.class);
        RunAs runAs = Common.getBean(RunAs.class);
        AtomicInteger eventCount = new AtomicInteger();

        runAs.runAs(runAs.systemSuperadmin(), () -> {
            List<IDataPoint> points = null;
            try {
                for(int i=0; i<params.tagsPerPoint; i++) {
                    params.tags.put("tag-" + i, UUID.randomUUID().toString());
                }
                points = params.mango.createMockDataPointsWithDetectors(params.dataPoints, params.tags, params.eventsPerEventDetector);
            } catch (ExecutionException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            for (IDataPoint point : points) {
                List<AbstractPointEventDetectorVO> detectors = EventDetectorDao.getInstance().getWithSource(point.getId(), (DataPointVO) point);
                if (detectors.size() > 0) {
                    DataPointEventType type = new DataPointEventType((DataPointVO) point, detectors.get(0));
                    HashMap context = new HashMap();
                    context.put(PointEventDetectorRT.EVENT_DETECTOR_CONTEXT_KEY, detectors.get(0));
                    context.put(PointEventDetectorRT.DATA_POINT_CONTEXT_KEY, point);

                    for (int i = 0; i < params.eventsPerEventDetector; i++) {
                        Common.eventManager.raiseEvent(type,
                                Common.timer.currentTimeMillis(),
                                false, AlarmLevels.INFORMATION,
                                new TranslatableMessage(LITERAL, BENCHMARK_DATA_POINT_EVENT_FOR + point.getName()),
                                context);

                        eventCount.getAndIncrement();
                    }
                }
            }
        });
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration(BenchmarkParams params) {
        super.tearDownIteration(params);
    }

    @TearDown(Level.Trial)
    public void tearDownTrial(BenchmarkParams params) {
        super.tearDownTrial(params);
    }

    @Benchmark
    @Threads(1)
    @Fork(1)
    @BenchmarkMode({Mode.SampleTime})
    @Measurement(iterations = 1, batchSize = 5)
    @Warmup(iterations = 0)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void queryEvents(BenchmarkParams params, Blackhole blackhole) throws IOException {
        AtomicInteger count = new AtomicInteger();
        params.eventInstanceService.list(evt -> {
            if(params.loadReadPermission) {
                evt.getReadPermission();
            }
            count.getAndIncrement();
            blackhole.consume(evt);
        });
        Assert.assertEquals(params.dataPoints, count.get());
    }

    @Benchmark
    @Threads(1)
    @Fork(1)
    @BenchmarkMode({Mode.SampleTime})
    @Measurement(iterations = 1, batchSize = 5)
    @Warmup(iterations = 0)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void queryEventsByDataPointTag(BenchmarkParams params, Blackhole blackhole) throws IOException {
        //TODO there is a bug where we cannot use the QueryBuilder from this service to query on tags
        AtomicInteger count = new AtomicInteger();
        String rql = params.tags.entrySet()
                .stream()
                .map(entry -> "eq(tags." + entry.getKey() + "," + entry.getValue() + ")")
                .collect(Collectors.joining("&"));

        params.eventInstanceService.query(rql, evt -> {
            if(params.loadReadPermission) {
                evt.getReadPermission();
            }
            count.getAndIncrement();
            blackhole.consume(evt);
        });

        Assert.assertEquals(params.dataPoints, count.get());
    }
}
