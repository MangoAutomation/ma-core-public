/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.MultistateStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.rt.maint.BackgroundProcessingImpl;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test the reliability of this detector within the runtime
 */
public class MultistateStateDetectorTest extends MangoTestBase {


    private EventDetectorEventListener listener;
    private MockDataSourceVO dsVo;

    private DataSourceService dataSourceService;
    private DataPointService dataPointService;
    private EventDetectorsService eventDetectorsService;

    @Before
    public void configure() {
        this.listener = new EventDetectorEventListener();
        Common.eventManager.addUserEventListener(this.listener);

        dataSourceService = Common.getBean(DataSourceService.class);
        dataPointService = Common.getBean(DataPointService.class);
        eventDetectorsService = Common.getBean(EventDetectorsService.class);

        dsVo = createMockDataSource(true);
    }

    @After
    public void tearDown() {
        // removes the data source from the RTM
        dataSourceService.delete(dsVo.getId());
    }

    @Test
    public void testAddDetectorWhilePointRunning() {
        DataPointVO dp = createDataPoint(true);

        DataPointRT rt = getRuntimeManager().getDataPoint(dp.getId());
        long valueTimestamp = timer.currentTimeMillis();
        ensureSetPointValue(rt, new PointValueTime(1, valueTimestamp));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        listener.assertNoEvents();

        //Add a detector
        MultistateStateDetectorVO ed = createEventDetector(dp);
        //the event will be raised when the detector is initialized
        ensureEventRaised(valueTimestamp);
        listener.raised.clear();
        listener.assertNoEvents();

        //Set value again (this detector only works on change)
        valueTimestamp = timer.currentTimeMillis();
        ensureSetPointValue(rt, new PointValueTime(1, valueTimestamp));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        listener.assertNoEvents();


    }

    protected DataPointVO createDataPoint(boolean enabled) {
        //Create point locator
        MockPointLocatorVO plVo = new MockPointLocatorVO(DataTypes.MULTISTATE, true);

        //Setup Data Point VO
        DataPointVO dpVo = new DataPointVO();
        dpVo.setName("Test");
        dpVo.setXid("DP_1");
        dpVo.setEnabled(enabled);
        dpVo.setLoggingType(DataPointVO.LoggingTypes.ALL);
        dpVo.setPointLocator(plVo);
        dpVo.setDataSourceId(dsVo.getId());

        return dataPointService.insert(dpVo);
    }

    /**
     * Create a detector for state 1
     */
    protected MultistateStateDetectorVO createEventDetector(DataPointVO dataPointVO) {
        PointEventDetectorDefinition<MultistateStateDetectorVO> multistateDef = ModuleRegistry.getEventDetectorDefinition(MultistateStateEventDetectorDefinition.TYPE_NAME);
        MultistateStateDetectorVO detector = multistateDef.baseCreateEventDetectorVO(dataPointVO);
        detector.setName(dataPointVO.getName());
        detector.setState(1);
        detector.setAlarmLevel(AlarmLevels.CRITICAL);
        eventDetectorsService.insertAndReload(detector, true);
        return detector;
    }

    /**
     * Save a value and ensure the event detector has received the event
     */
    protected void ensureSetPointValue(DataPointRT rt, PointValueTime value) {
        ConfirmedDataPointListener l = new ConfirmedDataPointListener();
        Common.runtimeManager.addDataPointListener(rt.getId(), l);
        rt.setPointValue(value, null);
        int retries = 20;
        while(retries > 0) {
            if(l.updated.get())
                break;
            else
                try{Thread.sleep(100);}catch(Exception e) {}
            retries--;
        }
        Common.runtimeManager.removeDataPointListener(rt.getId(), l);
        if(retries == 0)
            fail("did not receive updated event");
    }

    protected void ensureEventRaised(long timestamp) {
        int retries = 20;
        while(retries > 0) {
            boolean raised = false;
            for(EventInstance evt : listener.raised) {
                if(evt.getActiveTimestamp() == timestamp) {
                    raised = true;
                    break;
                }
            }
            if(raised)
                break;
            else
                try{Thread.sleep(100);}catch(Exception e) {}
            retries--;
        }

        if(retries == 0)
            fail("event not raised");
    }

    protected RuntimeManager getRuntimeManager() {
        return Common.getBean(RuntimeManager.class);
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        return new MockMangoLifecycle(modules) {
            @Override
            protected RuntimeManager getRuntimeManager() {
                return MultistateStateDetectorTest.this.getRuntimeManager();
            }

            @Override
            protected EventManager getEventManager() {
                return new EventManagerImpl();
            }

            @Override
            protected BackgroundProcessing getBackgroundProcessing() {
                return new BackgroundProcessingImpl();
            }
        };
    }

    class EventDetectorEventListener implements UserEventListener {

        List<EventInstance> raised = new ArrayList<>();
        List<EventInstance> rtn = new ArrayList<>();
        List<EventInstance> deactivated = new ArrayList<>();
        @Override
        public int getUserId() {
            return 1;
        }

        @Override
        public void raised(EventInstance evt) {
            raised.add(evt);
        }

        @Override
        public void returnToNormal(EventInstance evt) {
            rtn.add(evt);
        }

        @Override
        public void deactivated(EventInstance evt) {
            //Will happen after every test
            deactivated.add(evt);
        }

        @Override
        public void acknowledged(EventInstance evt) {
            fail("Should not happen");
        }

        public void assertRaised(int count) {
            assertEquals(count, raised.size());
        }
        public void assertRtn(int count) {
            assertEquals(count, rtn.size());
        }
        public void assertDeactivated(int count) {
            assertEquals(count, deactivated.size());
        }
        public void assertNoEvents() {
            assertRaised(0);
            assertRtn(0);
            assertDeactivated(0);
        }
    }

    class ConfirmedDataPointListener implements DataPointListener {

        AtomicBoolean changed = new AtomicBoolean();
        AtomicBoolean updated = new AtomicBoolean();
        AtomicBoolean logged = new AtomicBoolean();

        @Override
        public String getListenerName() {
            return "TEST";
        }

        @Override
        public void pointInitialized() {

        }

        @Override
        public void pointUpdated(PointValueTime newValue) {
            updated.set(true);
        }

        @Override
        public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
            this.changed.set(true);
        }

        @Override
        public void pointSet(PointValueTime oldValue, PointValueTime newValue) {

        }

        @Override
        public void pointBackdated(PointValueTime value) {

        }

        @Override
        public void pointTerminated(DataPointVO vo) {

        }

        @Override
        public void pointLogged(PointValueTime value) {
            logged.set(true);
        }

    }

}
