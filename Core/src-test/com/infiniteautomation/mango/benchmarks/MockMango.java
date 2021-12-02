/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.benchmarks;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;

/**
 * Base class for benchmarking Mango
 */
@State(Scope.Benchmark)
public class MockMango extends MangoTestBase {

    protected final MockMangoLifecycle lifecycle;

    public MockMango() {
        this.lifecycle = new MockMangoLifecycle(modules);
    }

    /**
     * Create points asynchronously and wait for all to be created
     */
    public List<DataPointVO> createDataPoints(int count, Map<String, String> tags) throws ExecutionException, InterruptedException {
        MockDataSourceVO ds = createMockDataSource();
        DataPointService service = Common.getBean(DataPointService.class);
        List<CompletableFuture<DataPointVO>> points = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            DataPointVO dp = new DataPointVO();
            dp.setName(UUID.randomUUID().toString());
            dp.setDeviceName(ds.getName());
            dp.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));
            dp.setDataSourceId(ds.getId());
            dp.setTags(tags);
            points.add(service.insertAsync(dp).toCompletableFuture());

        }
        return CompletableFuture.allOf(points.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> points.stream().map(CompletableFuture::join)
                        .collect(Collectors.toList())).get();
    }

    /**
     * Create data points and event detectors asynchronously, waiting until all are created
     */
    public List<DataPointVO> createMockDataPointsWithDetectors(int dataPointCount, Map<String, String> tags, int detectorsPerPoint) throws ExecutionException, InterruptedException {
        List<DataPointVO> points = createDataPoints(dataPointCount, tags);
        EventDetectorDefinition<?> updateEventDetectorDefinition = ModuleRegistry.getEventDetectorDefinition("UPDATE");
        List<CompletableFuture<AbstractEventDetectorVO>> detectors = new ArrayList<>();
        EventDetectorsService eventDetectorsService = Common.getBean(EventDetectorsService.class);
        for (IDataPoint point : points) {
            for (int i = 0; i < detectorsPerPoint; i++) {
                detectors.add(eventDetectorsService
                        .insertAsync(updateEventDetectorDefinition.baseCreateEventDetectorVO(point.getId()))
                        .toCompletableFuture());

            }
        }
        //Wait for all detectors to be created
        CompletableFuture.allOf(detectors.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> detectors.stream().map(CompletableFuture::join)
                        .collect(Collectors.toList())).get();
        return points;
    }


    @Override
    protected MockMangoLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Hook that runs before the lifecycle is initialized
     */
    protected void preInitialize() throws Exception {

    }

    /**
     * Common logic to initialize Mango before the trial.  Note this
     * will only work with Fork > 0.
     * @param setSecurityContext not used, simply pulls in {@link SetSecurityContext}
     */
    @Setup(Level.Trial)
    public void setupTrial(@SuppressWarnings("unused") SetSecurityContext setSecurityContext) throws Exception {
        long start = System.nanoTime();
        MangoTestBase.staticSetup();
        preInitialize();
        before();
        long duration = System.nanoTime() - start;
        System.out.printf("Initialized mock Mango in %.2f seconds.%n", TimeUnit.NANOSECONDS.toMillis(duration) / 1000.0);
    }

    /**
     * Common logic to shutdown Mango after a trial. Note this
     * will only work with Fork > 0
     */
    @TearDown(Level.Trial)
    public void tearDownTrial() throws IOException, SQLException {
        after();
        MangoTestBase.staticTearDown();
    }

    /**
     * Sets the security context in each thread
     */
    @State(Scope.Thread)
    public static class SetSecurityContext {
        @Setup
        public void setup() {
            MangoTestBase.setSuperadminAuthentication();
        }
    }
}
