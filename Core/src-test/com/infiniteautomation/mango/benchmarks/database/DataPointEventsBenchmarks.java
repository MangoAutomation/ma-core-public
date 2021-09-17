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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.infiniteautomation.mango.benchmarks.BenchmarkRunner;
import com.infiniteautomation.mango.benchmarks.MockMango;
import com.infiniteautomation.mango.spring.service.EventInstanceService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxyFactory;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.DefaultDatabaseProxyFactory;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

public class DataPointEventsBenchmarks extends BenchmarkRunner {

    public static final String LITERAL = "literal";
    public static final String BENCHMARK_DATA_POINT_EVENT_FOR = "Benchmark data point event for  ";

    public static class RealEventManagerMockMango extends MockMango {

        @Override
        protected void preInitialize() throws Exception {
            lifecycle.setEventManager(new EventManagerImpl());

            //Detect and set database if requested
            if(Common.envProps.getBoolean("db.benchmark", false)) {
                String type = Common.envProps.getString("db.type", "h2");
                DatabaseType databaseType = DatabaseType.valueOf(type.toUpperCase());
                DatabaseProxyFactory factory = new DefaultDatabaseProxyFactory();
                Common.databaseProxy = factory.createDatabaseProxy(databaseType);
                Common.databaseProxy.initialize(null);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class BenchmarkParams {

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

        public MockMango mango;

        @Setup(Level.Trial)
        public void setupTrial(RealEventManagerMockMango mockMango) {
            this.mango = mockMango;
            this.eventInstanceService = Common.getBean(EventInstanceService.class);
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            List<DataPointVO> points = null;
            try {
                for(int i=0; i<tagsPerPoint; i++) {
                    tags.put("tag-" + i, UUID.randomUUID().toString());
                }
                points = mango.createMockDataPointsWithDetectors(dataPoints, tags, eventsPerEventDetector);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            for (DataPointVO point : points) {
                List<AbstractPointEventDetectorVO> detectors = EventDetectorDao.getInstance().getWithSource(point.getId(), point);
                if (detectors.size() > 0) {
                    DataPointEventType type = new DataPointEventType(point, detectors.get(0));
                    HashMap<String, Object> context = new HashMap<>();
                    context.put(PointEventDetectorRT.EVENT_DETECTOR_CONTEXT_KEY, detectors.get(0));
                    context.put(PointEventDetectorRT.DATA_POINT_CONTEXT_KEY, point);

                    for (int i = 0; i < eventsPerEventDetector; i++) {
                        Common.eventManager.raiseEvent(type,
                                Common.timer.currentTimeMillis(),
                                false, AlarmLevels.INFORMATION,
                                new TranslatableMessage(LITERAL, BENCHMARK_DATA_POINT_EVENT_FOR + point.getName()),
                                context);
                    }
                }
            }
        }
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
