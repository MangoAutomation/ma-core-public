/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;
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
    AtomicInteger initialized = new AtomicInteger();
    List<PointValueTime> updated = new ArrayList<>();
    List<PointValueTime> changed = new ArrayList<>();
    List<PointValueTime> logged = new ArrayList<>();
    List<PointValueTime> backdated = new ArrayList<>();
    List<PointValueTime> set = new ArrayList<>();
    AtomicInteger terminated = new AtomicInteger();

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
        initialized.incrementAndGet();
    }

    public int getInitializedCount() {
        return initialized.get();
    }

    @Override
    public void pointUpdated(PointValueTime newValue) {
        updated.add(newValue);
    }

    public List<PointValueTime> getUpdated() {
        return updated;
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        changed.add(newValue);
    }

    public List<PointValueTime> getChanged() {
        return changed;
    }

    @Override
    public void pointLogged(PointValueTime value) {
        logged.add(value);
    }

    public List<PointValueTime> getLogged() {
        return logged;
    }

    @Override
    public void pointBackdated(PointValueTime value) {
        backdated.add(value);
    }

    public List<PointValueTime> getBackdated() {
        return backdated;
    }

    @Override
    public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
        set.add(newValue);
    }

    public List<PointValueTime> getSet() {
        return set;
    }

    @Override
    public void pointTerminated(DataPointVO vo) {
        terminated.incrementAndGet();
    }

    public int getTerminated() {
        return terminated.get();
    }


}
