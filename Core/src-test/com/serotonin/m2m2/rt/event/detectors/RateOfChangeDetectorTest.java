/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.definitions.event.detectors.RateOfChangeDetectorDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.RuntimeManagerImpl;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorTest extends MangoTestBase {

    private EventDetectorEventListener listener;
    
    //TODO Test with initial cache?
    //TODO Test backdates
    
    @Before
    public void configure() {
        this.listener = new EventDetectorEventListener();
        Common.eventManager.addUserEventListener(this.listener);
    }
    
    @After
    public void tearDown() {
        Common.eventManager.removeUserEventListener(this.listener);
    }
    
    @Test
    public void testFullRTIncreasingRateOfChangePeriod() {
        
        MockDataSourceVO dsVo = new MockDataSourceVO("test", "DS_1");
        dsVo.setEnabled(true);
        validate(dsVo);
        Common.runtimeManager.saveDataSource(dsVo);
        
        //Create point locator
        MockPointLocatorVO plVo = new MockPointLocatorVO(DataTypes.NUMERIC, true);
        
        //Setup Data Point VO
        DataPointVO dpVo = new DataPointVO();
        dpVo.setName("Test");
        dpVo.setXid("DP_1");
        dpVo.setEnabled(true);
        dpVo.setLoggingType(DataPointVO.LoggingTypes.ALL);
        dpVo.setPointLocator(plVo);
        dpVo.setDataSourceId(dsVo.getId());
        
        //Setup our ROC Detector
        RateOfChangeDetectorVO rocVo = new RateOfChangeDetectorVO(dpVo);
        rocVo.setDefinition(new RateOfChangeDetectorDefinition());
        rocVo.setDuration(1);
        rocVo.setDurationType(TimePeriods.SECONDS);
        List<AbstractPointEventDetectorVO<?>> eventDetectors = new ArrayList<>();
        eventDetectors.add(rocVo);
        dpVo.setEventDetectors(eventDetectors);
        
        validate(dpVo);
        Common.runtimeManager.saveDataPoint(dpVo);

        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());
        rt.setPointValue(new PointValueTime(10.0, timer.currentTimeMillis()), null);
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        
        //We need to watch for raised events
        System.out.print("Test");
        
        assertEquals(1, listener.raised.size());
        assertEquals(1000, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
    }
    
    @Override
    protected MockMangoLifecycle getLifecycle() {
        RuntimeManagerMockMangoLifecycle lifecycle =
                new RuntimeManagerMockMangoLifecycle(modules, enableH2Web, h2WebPort);
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
        
        @Override
        protected EventManager getEventManager() {
            return new EventManagerImpl();
        }

    }
    
    class EventDetectorEventListener implements UserEventListener {
        
        List<EventInstance> raised = new ArrayList<>();
        List<EventInstance> rtn = new ArrayList<>();
        
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
            fail("Should not happen");
        }

        @Override
        public void acknowledged(EventInstance evt) {
            fail("Should not happen");
        }
    }

}
