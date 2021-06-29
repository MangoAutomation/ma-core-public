/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.util.concurrent.atomic.AtomicInteger;

import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 *
 * @author Terry Packer
 */
public class TestDataPointListener implements DataPointListener {
    int dataPointId;
    String name;
    AtomicInteger eventCount = new AtomicInteger();

    public TestDataPointListener(String name, int dataPointId) {
        this.name = name;
        this.dataPointId = dataPointId;
    }
    
    @Override
    public String getListenerName() {
        return name;
    }

    @Override
    public void pointInitialized() {
        eventCount.incrementAndGet();
    }

    @Override
    public void pointUpdated(PointValueTime newValue) {
        eventCount.incrementAndGet();
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        eventCount.incrementAndGet();
    }

    @Override
    public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
        eventCount.incrementAndGet();
    }

    @Override
    public void pointBackdated(PointValueTime value) {
        eventCount.incrementAndGet();
    }

    @Override
    public void pointTerminated(DataPointVO vo) {
        eventCount.incrementAndGet();
    }

    @Override
    public void pointLogged(PointValueTime value) {
        eventCount.incrementAndGet();
    }

}
