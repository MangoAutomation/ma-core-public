/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.definitions.event.detectors.RateOfChangeDetectorDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.RuntimeManagerImpl;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.ComparisonMode;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorTest extends MangoTestBase {

    private EventDetectorEventListener listener;
        
    @Before
    public void configure() {
        this.listener = new EventDetectorEventListener();
        Common.eventManager.addUserEventListener(this.listener);
    }
    
    @After
    public void tearDown() {
        Common.eventManager.removeUserEventListener(this.listener);
        Common.eventManager = new SimpleEventManager();
    }
    
    /**
     * Test a single value that does not cause a detected change at the end of the first period
     */
    @Test
    public void testOneSecondPeriodSingleValueOutOfRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, false, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);

        ensureSetPointValue(rt, new PointValueTime(10.0, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    /**
     * Test a single value that does not cause a detected change at the end of the first period
     */
    @Test
    public void testOneSecondPeriodSingleValueInRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, false, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    /**
     * Test 2 values that causes a detected change within the first period
     */
    @Test
    public void testOneSecondPeriodTwoValuesOutOfRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, false, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);

        ensureSetPointValue(rt, new PointValueTime(0.0, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(1.1, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
                
        assertEquals(1, listener.raised.size());
        assertEquals(500, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
    }
    
    /**
     * Test 2 values that do not cause a detected change within the first period
     */
    @Test
    public void testOneSecondPeriodTwoValuesInRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, false, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    
    /**
     * Test 2 values that do not cause a detected change within the first period
     */
    @Test
    public void testLessThanChange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, false, 1, TimePeriods.SECONDS, ComparisonMode.LESS_THAN, 1, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(1, listener.raised.size());
        assertEquals(1000, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
    }
    
    
    /**
     * Test 2 values in the database and then set 2 values that do not cause a detected change within the first period
     */
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesInRange() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, null, false, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.1, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.3, 100), null);
        timer.fastForwardTo(timer.currentTimeMillis() + 200);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }

    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesOutInRange() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, null, false, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.9, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(1.1, 100), null);
        timer.fastForwardTo(timer.currentTimeMillis() + 200);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesOutOfRangeReset() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, 0.9, false, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(1.1, 100), null);
        timer.fastForwardTo(200);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        timer.fastForwardTo(500);
        
        assertEquals(1, listener.raised.size());
        assertEquals(100, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(1000);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(1, listener.rtn.size());
        assertEquals(500, listener.rtn.get(0).getRtnTimestamp());
    }
    
    @Test
    public void testNoValuesSetTwoInitialValues() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, 1.1, false, 1, TimePeriods.SECONDS, ComparisonMode.LESS_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.5, 500), null);
        timer.fastForwardTo(1000);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        timer.fastForwardTo(1000);
        
        
        assertEquals(1, listener.raised.size());
        assertEquals(500, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());

        ensureSetPointValue(rt, new PointValueTime(2.2, 1000));
        timer.fastForwardTo(1100);
        
        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());
        assertEquals(1000, listener.rtn.get(0).getRtnTimestamp());
    }
    
    //TODO TEst no values set, 1 initial value
    //TODO Test no values set, 0 initial values
    //TODO Time period tests for duration
    //TEST GTET and LTET
    
    /**
     * Create a running datapoint with no initial values
     * @param change - allowable change
     * @param periods - periods for change
     * @param periodType - period type for change
     * @return
     */
    protected DataPointRT createRunningPoint(double change, Double resetRange, boolean useAbsoluteValue, int rocDuration, int rocDurationType, ComparisonMode comparisonMode, int periods, int periodType) {
        DataPointVO dpVo = createDisabledPoint(change, resetRange, useAbsoluteValue, rocDuration, rocDurationType, comparisonMode, periods, periodType);
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        return Common.runtimeManager.getDataPoint(dpVo.getId());
    }
    
    /**
     * Create a point in the database that is not running
     * 
     * @param change
     * @param rocDuration
     * @param rocDurationType
     * @param notHigher
     * @param periods
     * @param periodType
     * @return
     */
    protected DataPointVO createDisabledPoint(double rocThreshold, Double resetThreshold, boolean useAbsoluteValue, int rocDuration, int rocDurationType, ComparisonMode comparisonMode, int periods, int periodType) {
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
        dpVo.setEnabled(false);
        dpVo.setLoggingType(DataPointVO.LoggingTypes.ALL);
        dpVo.setPointLocator(plVo);
        dpVo.setDataSourceId(dsVo.getId());
        
        //Setup our ROC Detector
        RateOfChangeDetectorVO rocVo = new RateOfChangeDetectorVO(dpVo);
        rocVo.setXid(EventDetectorDao.getInstance().generateUniqueXid());
        rocVo.setDefinition(new RateOfChangeDetectorDefinition());
        rocVo.setRateOfChangeThreshold(rocThreshold);
        rocVo.setResetThreshold(resetThreshold);
        rocVo.setRateOfChangeDurationPeriods(rocDuration);
        rocVo.setRateOfChangeDurationType(rocDurationType);
        rocVo.setComparisonMode(comparisonMode);
        rocVo.setUseAbsoluteValue(useAbsoluteValue);
        rocVo.setDuration(periods);
        rocVo.setDurationType(periodType);
        validate(rocVo);
        
        List<AbstractPointEventDetectorVO<?>> eventDetectors = new ArrayList<>();
        eventDetectors.add(rocVo);
        dpVo.setEventDetectors(eventDetectors);
        
        validate(dpVo);
        Common.runtimeManager.saveDataPoint(dpVo);
        return dpVo;
    }
    
    /**
     * Save a value and ensure the event detector has received the event
     * @param id
     * @param value
     */
    private void ensureSetPointValue(DataPointRT rt, PointValueTime value) {
        ConfirmedDataPointListener l = new ConfirmedDataPointListener();
        Common.runtimeManager.addDataPointListener(rt.getId(), l);
        rt.setPointValue(value, null);
        int retries = 10;
        while(retries > 0) {
            if(l.changed.get())
                break;
            else
                try{Thread.sleep(100);}catch(Exception e) {}
            retries--;
        }
        Common.runtimeManager.removeDataPointListener(rt.getId(), l);
        if(retries == 0)
            fail("did not recieved changed event");
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
            return new SimpleEventManager();
        }
    }
    
    class SimpleEventManager extends MockEventManager {
        
        private List<EventDetectorEventListener> listeners = new ArrayList<>();
        private List<EventInstance> events = new ArrayList<>();
        
        @Override
        public void raiseEvent(EventType type, long time, boolean rtnApplicable,
                AlarmLevels alarmLevel, TranslatableMessage message, Map<String, Object> context) {
            if(!(type instanceof DataPointEventType))
                return;
            EventInstance evt = new EventInstance(type, time, rtnApplicable,
                    alarmLevel, message, context);
            events.add(evt);
            for(EventDetectorEventListener l : listeners)
               l.raised(evt);
        }
        
        @Override
        public void returnToNormal(EventType type, long time) {
            returnToNormal(type, time, ReturnCause.RETURN_TO_NORMAL);
        }
        
        @Override
        public void returnToNormal(EventType type, long time, ReturnCause cause) {
            if(!(type instanceof DataPointEventType))
                return;
            EventInstance evt = remove(type);
            if(evt == null)
                return;
            evt.returnToNormal(time, cause);
            for(EventDetectorEventListener l : listeners)
                l.returnToNormal(evt);
        }
        
        private EventInstance remove(EventType type) {
            ListIterator<EventInstance> it = events.listIterator();
            while(it.hasNext()){
                EventInstance e = it.next();
                if (e.getEventType().equals(type)) {
                    it.remove();
                    return e;
                }
            }
            return null;
        }
        
        @Override
        public void addUserEventListener(UserEventListener l) {
            this.listeners.add((EventDetectorEventListener) l);
        }
        
        @Override
        public void removeUserEventListener(UserEventListener l) {
            this.listeners.remove(l);
        }
        
    }
    
    class ConfirmedDataPointListener implements DataPointListener {

        AtomicBoolean changed = new AtomicBoolean();
        
        @Override
        public String getListenerName() {
            return "TEST";
        }

        @Override
        public void pointInitialized() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void pointUpdated(PointValueTime newValue) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
            this.changed.set(true);
        }

        @Override
        public void pointSet(PointValueTime oldValue, PointValueTime newValue) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void pointBackdated(PointValueTime value) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void pointTerminated(DataPointVO vo) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void pointLogged(PointValueTime value) {
            // TODO Auto-generated method stub
            
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
