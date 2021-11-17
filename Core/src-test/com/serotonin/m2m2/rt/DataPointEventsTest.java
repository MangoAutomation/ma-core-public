/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Test;

import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.serotonin.json.JsonException;
import com.serotonin.m2m2.CallingThreadMockBackgroundProcessing;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

/**
 *
 * @author Terry Packer
 */
public class DataPointEventsTest extends MangoTestBase {

    @After
    public void stopAllDataSources() {
        //Since we don't shutdown the runtime between tests we need to do this
        DataSourceService service = Common.getBean(DataSourceService.class);
        service.list(ds -> service.delete(ds.getId()));
    }

    @Test
    public void testDataPointEventFirstValue() {

        MockDataSourceVO ds = createMockDataSource(true);
        //Note the default is to have a logging type of ON_CHANGE
        DataPointVO point = createMockDataPoint(ds, new MockPointLocatorVO(DataTypes.MULTISTATE, true), true);

        TestDataPointListener l = new TestDataPointListener("Listener One", point.getId());
        Common.runtimeManager.addDataPointListener(l.dataPointId, l);

        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());

        //Ensure there is no current value
        assertEquals(rt.getPointValue(), null);

        //Set the value to 1
        PointValueTime valueSet = new PointValueTime(1, Common.timer.currentTimeMillis());
        rt.setPointValue(valueSet, null);
        assertEquals(1, rt.getPointValue().getValue().getIntegerValue());

        //Test first value set
        assertEquals(1, l.getUpdated().size());
        assertEquals(valueSet.getIntegerValue(), l.getUpdated().get(0).getValue().getIntegerValue());
        assertEquals(1, l.getChanged().size());
        assertEquals(1, l.getLogged().size());
        assertEquals(valueSet.getIntegerValue(), l.getLogged().get(0).getValue().getIntegerValue());
        assertEquals(0, l.getBackdated().size());
        assertEquals(0, l.getSet().size());
    }

    @Test
    public void testDataPointEventUpdated() {

        MockDataSourceVO ds = createMockDataSource(true);
        //Note the default is to have a logging type of ON_CHANGE
        DataPointVO point = createMockDataPoint(ds, new MockPointLocatorVO(DataTypes.MULTISTATE, true), true);

        TestDataPointListener l = new TestDataPointListener("Listener One", point.getId());
        Common.runtimeManager.addDataPointListener(l.dataPointId, l);

        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());

        //Ensure there is no current value
        assertEquals(rt.getPointValue(), null);

        //Set the value to 1
        PointValueTime valueSet = new PointValueTime(1, Common.timer.currentTimeMillis());
        rt.setPointValue(valueSet, null);
        assertEquals(1, rt.getPointValue().getValue().getIntegerValue());

        //Test first value set
        assertEquals(1, l.getUpdated().size());
        assertEquals(valueSet.getIntegerValue(), l.getUpdated().get(0).getValue().getIntegerValue());
        assertEquals(1, l.getChanged().size());
        assertEquals(1, l.getLogged().size());
        assertEquals(valueSet.getIntegerValue(), l.getLogged().get(0).getValue().getIntegerValue());
        assertEquals(0, l.getBackdated().size());
        assertEquals(0, l.getSet().size());


        Common.runtimeManager.removeDataPointListener(l.dataPointId, l);
        l = new TestDataPointListener("Listener Two", point.getId());
        Common.runtimeManager.addDataPointListener(l.dataPointId, l);

        //Set to same value again
        rt.setPointValue(valueSet, null);
        assertEquals(rt.getPointValue().getValue().getIntegerValue(), 1);

        assertEquals(1, l.getUpdated().size());
        assertEquals(valueSet.getIntegerValue(), l.getUpdated().get(0).getValue().getIntegerValue());
        assertEquals(0, l.getChanged().size());
        assertEquals(0, l.getLogged().size());
        assertEquals(0, l.getBackdated().size());
        assertEquals(0, l.getSet().size());
    }

    @Test
    public void testDataPointEventChanged() {

        MockDataSourceVO ds = createMockDataSource(true);
        //Note the default is to have a logging type of ON_CHANGE
        DataPointVO point = createMockDataPoint(ds, new MockPointLocatorVO(DataTypes.MULTISTATE, true), true);

        TestDataPointListener l = new TestDataPointListener("Listener One", point.getId());
        Common.runtimeManager.addDataPointListener(l.dataPointId, l);

        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());

        //Ensure there is no current value
        assertEquals(rt.getPointValue(), null);

        //Set the value to 1
        PointValueTime valueSet = new PointValueTime(1, Common.timer.currentTimeMillis());
        rt.setPointValue(valueSet, null);
        assertEquals(1, rt.getPointValue().getValue().getIntegerValue());

        //Test first value set
        assertEquals(1, l.getUpdated().size());
        assertEquals(valueSet.getIntegerValue(), l.getUpdated().get(0).getValue().getIntegerValue());
        assertEquals(1, l.getChanged().size());
        assertEquals(1, l.getLogged().size());
        assertEquals(valueSet.getIntegerValue(), l.getLogged().get(0).getValue().getIntegerValue());
        assertEquals(0, l.getBackdated().size());
        assertEquals(0, l.getSet().size());

        Common.runtimeManager.removeDataPointListener(l.dataPointId, l);
        l = new TestDataPointListener("Listener Two", point.getId());
        Common.runtimeManager.addDataPointListener(l.dataPointId, l);

        //Set to new value
        valueSet = new PointValueTime(2, Common.timer.currentTimeMillis());
        rt.setPointValue(valueSet, null);
        assertEquals(2, rt.getPointValue().getValue().getIntegerValue());

        assertEquals(1, l.getUpdated().size());
        assertEquals(valueSet.getIntegerValue(), l.getUpdated().get(0).getValue().getIntegerValue());
        assertEquals(1, l.getChanged().size());
        assertEquals(valueSet.getIntegerValue(), l.getChanged().get(0).getValue().getIntegerValue());
        assertEquals(1, l.getLogged().size());
        assertEquals(valueSet.getIntegerValue(), l.getLogged().get(0).getValue().getIntegerValue());
        assertEquals(0, l.getBackdated().size());
        assertEquals(0, l.getSet().size());
    }

    @Test
    public void testDataPointEventBackdated() {

        MockDataSourceVO ds = createMockDataSource(true);
        //Note the default is to have a logging type of ON_CHANGE
        DataPointVO point = createMockDataPoint(ds, new MockPointLocatorVO(DataTypes.MULTISTATE, true), true);

        TestDataPointListener l = new TestDataPointListener("Listener One", point.getId());
        Common.runtimeManager.addDataPointListener(l.dataPointId, l);

        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());

        //Ensure there is no current value
        assertEquals(rt.getPointValue(), null);

        //Set the value to 1 at some known time
        long valueTime = 10000;
        PointValueTime valueSet = new PointValueTime(1, valueTime);
        rt.setPointValue(valueSet, null);
        assertEquals(1, rt.getPointValue().getValue().getIntegerValue());

        //Test first value set
        assertEquals(1, l.getUpdated().size());
        assertEquals(valueSet.getIntegerValue(), l.getUpdated().get(0).getValue().getIntegerValue());
        assertEquals(1, l.getChanged().size());
        assertEquals(1, l.getLogged().size());
        assertEquals(valueSet.getIntegerValue(), l.getLogged().get(0).getValue().getIntegerValue());
        assertEquals(0, l.getBackdated().size());
        assertEquals(0, l.getSet().size());


        Common.runtimeManager.removeDataPointListener(l.dataPointId, l);
        l = new TestDataPointListener("Listener Two", point.getId());
        Common.runtimeManager.addDataPointListener(l.dataPointId, l);

        //Set to new value that is  backdated
        valueSet = new PointValueTime(2, valueTime - 1000);
        rt.setPointValue(valueSet, null);
        assertEquals(1, rt.getPointValue().getValue().getIntegerValue());

        assertEquals(0, l.getUpdated().size());
        assertEquals(0, l.getChanged().size());
        //On change backdates are ignored
        assertEquals(0, l.getLogged().size());
        assertEquals(1, l.getBackdated().size());
        assertEquals(valueSet.getIntegerValue(), l.getBackdated().get(0).getValue().getIntegerValue());
        assertEquals(0, l.getSet().size());
    }

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
        return new MockMangoLifecycle(modules) {
            @Override
            protected RuntimeManager getRuntimeManager() {
                return Common.getBean(RuntimeManager.class);
            }

            @Override
            protected BackgroundProcessing getBackgroundProcessing() {
                return new CallingThreadMockBackgroundProcessing();
            }
        };

    }

}
