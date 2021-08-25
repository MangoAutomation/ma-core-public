/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import java.util.List;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.vo.IDataPoint;

/**
 * Base class for benchmarking Mango
 */
public class MockMango extends MangoTestBase {

    public List<IDataPoint> createMockDataPoints(int count){
        return super.createMockDataPoints(count);
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifeycle = new MockMangoLifecycle(modules);
        //TODO set database type
        return lifeycle;
    }
}
