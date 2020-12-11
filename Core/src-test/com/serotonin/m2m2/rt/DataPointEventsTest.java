/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

/**
 *
 * @author Terry Packer
 */
public class DataPointEventsTest extends MangoTestBase {

    @Test
    public void testListenerAddRemoveSyncrhonization()
            throws InterruptedException, JsonException, IOException, URISyntaxException {

        MockDataSourceVO ds = createMockDataSource(true);
        DataPointVO point = createMockDataPoint(ds, new MockPointLocatorVO(DataTypes.NUMERIC, true), true);


        int inserted = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(3);
        SynchronousQueue<TestDataPointListener> queue = new SynchronousQueue<>();

        TestDataPointListener l = new TestDataPointListener("Listener One", point.getId());

        AtomicBoolean removerRunning = new AtomicBoolean(true);
        AtomicBoolean generatorRunning = new AtomicBoolean(true);
        Runnable adder = () -> {
            while (removerRunning.get()) {
                try {
                    TestDataPointListener listener = queue.take();
                    Common.runtimeManager.addDataPointListener(listener.dataPointId, listener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Runnable remover = () -> {
            while (generatorRunning.get()) {
                try {
                    queue.put(l);
                    Common.runtimeManager.removeDataPointListener(l.dataPointId, l);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            removerRunning.set(false);
        };

        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());
        // Value Generator Thread
        Runnable generator = () -> {
            // Save some values
            List<PointValueTime> values = new ArrayList<>();
            for (int i = 0; i < inserted; i++) {
                PointValueTime newValue = new PointValueTime(1.0, timer.currentTimeMillis());
                rt.setPointValue(newValue, null);
                values.add(newValue);
                timer.fastForwardTo(timer.currentTimeMillis() + 1);
            }
            generatorRunning.set(false);
        };
        executor.execute(adder);
        executor.execute(remover);
        executor.execute(generator);
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        executor.shutdown();
    }


    @Override
    protected MockMangoLifecycle getLifecycle() {
        RuntimeManagerMockMangoLifecycle lifecycle =
                new RuntimeManagerMockMangoLifecycle(modules);
        return lifecycle;
    }

    class RuntimeManagerMockMangoLifecycle extends MockMangoLifecycle {

        /**
         * @param modules
         */
        public RuntimeManagerMockMangoLifecycle(List<Module> modules) {
            super(modules);
        }

        @Override
        protected RuntimeManager getRuntimeManager() {
            return new RuntimeManagerImpl();
        }

    }
}
