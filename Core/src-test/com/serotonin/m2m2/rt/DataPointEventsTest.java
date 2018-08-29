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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 *
 * @author Terry Packer
 */
public class DataPointEventsTest extends MangoTestBase {

    @Test
    public void testListenerAddRemoveSyncrhonization() throws InterruptedException, JsonException, IOException, URISyntaxException {
        
        loadDefaultConfiguration();
        AtomicInteger eventCount = new AtomicInteger();
        int inserted = 1000;
        DataPointVO point = DataPointDao.getInstance().getByXid("temperature");
        ExecutorService executor = Executors.newFixedThreadPool(3);
        SynchronousQueue<DataPointListener> queue = new SynchronousQueue<>();
        
        DataPointListener l = new DataPointListener() {
            
            @Override
            public String getListenerName() {
                return "test listener";
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
            public void pointTerminated() {
                eventCount.incrementAndGet();
            }

            @Override
            public void pointLogged(PointValueTime value) {
                eventCount.incrementAndGet();
            }
            
        };

        AtomicBoolean removerRunning = new AtomicBoolean(true);
        AtomicBoolean generatorRunning = new AtomicBoolean(true);
        Runnable adder = () -> {
            while(removerRunning.get()) {
                try {
                    queue.take();
                    Common.runtimeManager.addDataPointListener(point.getId(), l);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Runnable remover = () -> {
            while(generatorRunning.get()) {
                try {
                    queue.put(l);
                    Common.runtimeManager.removeDataPointListener(point.getId(), l);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
            removerRunning.set(false);
        };
        
        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());
        //Value Generator Thread
        Runnable generator = () -> {
            //Save some values
            List<PointValueTime> values = new ArrayList<>();
            for(int i=0; i<inserted; i++) {
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
        RuntimeManagerMockMangoLifecycle lifecycle = new RuntimeManagerMockMangoLifecycle(modules, enableH2Web, h2WebPort);
        return lifecycle;
    }
    
    class RuntimeManagerMockMangoLifecycle extends MockMangoLifecycle {

        /**
         * @param modules
         * @param enableWebConsole
         * @param webPort
         */
        public RuntimeManagerMockMangoLifecycle(List<Module> modules, boolean enableWebConsole,
                int webPort) {
            super(modules, enableWebConsole, webPort);
        }
        @Override
        protected RuntimeManager getRuntimeManager() {
            return new RuntimeManagerImpl();
        }
        
    }
}
