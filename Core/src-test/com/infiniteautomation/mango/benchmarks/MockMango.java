/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.benchmarks;

import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;

/**
 * Base class for benchmarking Mango
 */
public class MockMango extends MangoTestBase {

    private final MockMangoLifecycle lifeycle;

    public MockMango() {
        this.lifeycle = new MockMangoLifecycle(modules);
    }


    /**
     * Create points asynchronously and wait for all to be created
     *
     * @param count
     * @param tags
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<IDataPoint> createDataPoints(int count, Map<String, String> tags) throws ExecutionException, InterruptedException {
        MockDataSourceVO ds = createMockDataSource();
        DataPointService service = Common.getBean(DataPointService.class);
        List<CompletableFuture<DataPointVO>> points = new ArrayList<>();

        for(int i=0; i<count; i++) {
            DataPointVO dp = new DataPointVO();
            dp.setName(UUID.randomUUID().toString());
            dp.setDeviceName(ds.getName());
            dp.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));
            dp.setDataSourceId(ds.getId());
            dp.setTags(tags);
            points.add(service.insertAsync(dp).toCompletableFuture());

        }
        return CompletableFuture.allOf(points.toArray(new CompletableFuture[points.size()]))
                .thenApply(ignored -> points.stream().map(f -> (IDataPoint)f.join())
                .collect(Collectors.toList())).get();
    }

    /**
     * Create data points and event detectors asynchronously, waiting until all are created
     *
     * @param dataPointCount
     * @param tags
     * @param detectorsPerPoint
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<IDataPoint> createMockDataPointsWithDetectors(int dataPointCount, Map<String, String> tags, int detectorsPerPoint) throws ExecutionException, InterruptedException {
        List<IDataPoint> points = createDataPoints(dataPointCount, tags);
        EventDetectorDefinition<?> updateEventDetectorDefinition = ModuleRegistry.getEventDetectorDefinition("UPDATE");
        List<CompletableFuture<AbstractEventDetectorVO>> detectors = new ArrayList<>();
        EventDetectorsService eventDetectorsService = Common.getBean(EventDetectorsService.class);
        for (IDataPoint point : points) {
            for(int i=0; i<detectorsPerPoint; i++) {
                detectors.add(eventDetectorsService
                        .insertAsync(updateEventDetectorDefinition.baseCreateEventDetectorVO(point.getId()))
                        .toCompletableFuture());

            }
        }
        //Wait for all detectors to be created
        CompletableFuture.allOf(detectors.toArray(new CompletableFuture[detectors.size()]))
                .thenApply(ignored -> detectors.stream().map(f -> (AbstractEventDetectorVO)f.join())
                        .collect(Collectors.toList())).get();
        return points;
    }


    @Override
    protected MockMangoLifecycle getLifecycle() {
        return lifeycle;
    }

    /**
     * Drop and re-create all tables
     */
    public void resetDatabase() {
        if(Common.databaseProxy instanceof H2InMemoryDatabaseProxy) {
            H2InMemoryDatabaseProxy proxy = (H2InMemoryDatabaseProxy) Common.databaseProxy;
            try {
                proxy.clean();
            } catch (Exception e) {
                throw new ShouldNeverHappenException(e);
            }
        }else {
            try {
                String databaseName = Common.databaseProxy.getDataSource().getConnection().getCatalog();
                Common.databaseProxy.getDataSource().getConnection().createStatement().executeUpdate("DROP DATABASE `" + databaseName + "`");
                Common.databaseProxy.getDataSource().getConnection().createStatement().executeUpdate("CREATE DATABASE `" + databaseName + "`");
                Common.databaseProxy.initialize(null);
                //TODO Reset caches...
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * Set the event manager implementation
     * @param eventManager
     */
    public void setEventManager(EventManager eventManager) {
        this.lifeycle.setEventManager(eventManager);
    }
}
