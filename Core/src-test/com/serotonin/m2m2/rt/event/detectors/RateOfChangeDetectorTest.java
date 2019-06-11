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
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.CalculationMode;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.ComparisonMode;

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

    @Test
    public void testOneSecondPeriodSingleValueOutOfRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);

        ensureSetPointValue(rt, new PointValueTime(10.0, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }

    @Test
    public void testOneSecondPeriodSingleValueInRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testOneSecondPeriodTwoValuesOutOfRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);

        ensureSetPointValue(rt, new PointValueTime(0.0, timer.currentTimeMillis()));
        timer.fastForwardTo(500);
        ensureSetPointValue(rt, new PointValueTime(1.1, timer.currentTimeMillis()));
        
        timer.fastForwardTo(1000);
        assertEquals(1, listener.raised.size());
        assertEquals(500, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
        
        //Our event will RTN instantaneously when we set a value
        timer.fastForwardTo(5000);
        assertEquals(1, listener.raised.size());
        assertEquals(0, listener.rtn.size());
        
        ensureSetPointValue(rt, new PointValueTime(1.2, timer.currentTimeMillis()));
        timer.fastForwardTo(5001);
        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());
        assertEquals(5000, listener.rtn.get(0).getRtnTimestamp());
    }

    @Test
    public void testOneSecondPeriodTwoValuesInRange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testLessThanChange() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.LESS_THAN, 1, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(1, listener.raised.size());
        assertEquals(1000, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesInRange() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN,  0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.001, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0003, 100), null);
        timer.fastForwardTo(timer.currentTimeMillis() + 200);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        ensureSetPointValue(rt, new PointValueTime(0.0005, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.0009, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesOutOfRangeForOneSecond() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN_OR_EQUALS, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.1, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(1.101, 100), null);
        timer.fastForwardTo(200);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        ensureSetPointValue(rt, new PointValueTime(1.5, timer.currentTimeMillis()));
        timer.fastForwardTo(500);
        ensureSetPointValue(rt, new PointValueTime(2.5, timer.currentTimeMillis()));
        timer.fastForwardTo(1100); //1s after the change of grater than 1
        assertEquals(1, listener.raised.size());
        assertEquals(200, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());        

        ensureSetPointValue(rt, new PointValueTime(2.2, timer.currentTimeMillis()));
        timer.fastForwardTo(4500); //Well after we have had no changes and our RoC has dropped to RTN the event
        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());
        assertEquals(1100, listener.rtn.get(0).getRtnTimestamp());
    }
    
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesOutOfRangeReset() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, 0.9, 1, TimePeriods.SECONDS, false,  CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN,  0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(1.1, 100), null);
        timer.fastForwardTo(1000);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
        
        timer.fastForwardTo(1500);
        ensureSetPointValue(rt, new PointValueTime(2.1, timer.currentTimeMillis()));
        timer.fastForwardTo(1501);
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());

        timer.fastForwardTo(1600);
        ensureSetPointValue(rt, new PointValueTime(3.2, timer.currentTimeMillis()));
        timer.fastForwardTo(1601);
        assertEquals(1, listener.raised.size());
        assertEquals(1600, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
        
        timer.fastForwardTo(2000);
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(2001);
        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());
        assertEquals(2000, listener.rtn.get(0).getRtnTimestamp());
        
        
        //See RoC Return to 0
        timer.fastForwardTo(4500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(5000);
        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());

    }
    
    @Test
    public void testNoValuesSetTwoInitialValues() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, 1.1, 1, TimePeriods.SECONDS, false, CalculationMode.INSTANTANEOUS, 0, TimePeriods.SECONDS, ComparisonMode.LESS_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.5, 500), null);
        timer.fastForwardTo(1000);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        //An event will be active as the RoC is 0 at startup and < 1.0
        assertEquals(1, listener.raised.size());
        assertEquals(500, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());

        ensureSetPointValue(rt, new PointValueTime(2.2, timer.currentTimeMillis()));
        timer.fastForwardTo(1200);

        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());
        assertEquals(1000, listener.rtn.get(0).getRtnTimestamp());
    }
    
    //Average Mode Tests
    @Test
    public void testOneSecondPeriodSingleValueOutOfRangeAverage() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);

        ensureSetPointValue(rt, new PointValueTime(10.0, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }

    @Test
    public void testOneSecondPeriodSingleValueInRangeAverage() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 5000);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testOneSecondPeriodTwoValuesOutOfRangeAverage() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);

        ensureSetPointValue(rt, new PointValueTime(0.0, timer.currentTimeMillis()));
        timer.fastForwardTo(500);
        ensureSetPointValue(rt, new PointValueTime(1.1, timer.currentTimeMillis()));
        
        timer.fastForwardTo(1000);
        assertEquals(1, listener.raised.size());
        assertEquals(500, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
        
        //Our event will RTN as the period slides along and our RoC returns to 0
        timer.fastForwardTo(5000);
        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());
        assertEquals(1500, listener.rtn.get(0).getRtnTimestamp());
    }
    
    @Test
    public void testOneSecondPeriodTwoValuesInRangeAverage() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testLessThanChangeAverage() {
        
        DataPointRT rt = createRunningPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.LESS_THAN, 1, TimePeriods.SECONDS);
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(1, listener.raised.size());
        assertEquals(1000, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesInRangeAverage() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.001, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0003, 100), null);
        timer.fastForwardTo(timer.currentTimeMillis() + 200);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        ensureSetPointValue(rt, new PointValueTime(0.0005, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 500);
        ensureSetPointValue(rt, new PointValueTime(0.0009, timer.currentTimeMillis()));
        timer.fastForwardTo(timer.currentTimeMillis() + 4500);
        
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
    }
    
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesOutOfRangeForOneSecondAverage() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, null, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 1, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.1, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(1.101, 100), null);
        timer.fastForwardTo(1000);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        assertEquals(0, listener.raised.size());
        
        ensureSetPointValue(rt, new PointValueTime(2.5, timer.currentTimeMillis()));
        timer.fastForwardTo(1001);
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());   
        
        timer.fastForwardTo(1100);
        assertEquals(1, listener.raised.size());
        assertEquals(1100, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());   
        
        
        ensureSetPointValue(rt, new PointValueTime(2.9, timer.currentTimeMillis()));
        timer.fastForwardTo(2500); //1s after the change of greater than 1
        assertEquals(1, listener.raised.size());
        assertEquals(1, listener.rtn.size());
        assertEquals(1100, listener.rtn.get(0).getRtnTimestamp());    
    }
    
    @Test
    public void testOneSecondPeriodTwoInitialValuesTwoValuesOutOfRangeResetAverage() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, 0.9, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(1.1, 100), null);
        timer.fastForwardTo(1000);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());

        timer.fastForwardTo(1500);
        
        assertEquals(1, listener.raised.size());
        assertEquals(100, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());
        
        ensureSetPointValue(rt, new PointValueTime(0.5, timer.currentTimeMillis()));
        timer.fastForwardTo(2000);
        ensureSetPointValue(rt, new PointValueTime(0.9, timer.currentTimeMillis()));
        timer.fastForwardTo(4500);
        
        assertEquals(1, listener.rtn.size());
        assertEquals(1500, listener.rtn.get(0).getRtnTimestamp());
    }
    
    @Test
    public void testNoValuesSetTwoInitialValuesAverage() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, 1.1, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 1, TimePeriods.SECONDS, ComparisonMode.LESS_THAN, 0, TimePeriods.SECONDS);
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
    
    /**
     * Change of 1.0/s in 10 seconds
     */
    @Test
    public void testDifferentThresholdPeriodToAverage() {
        
        DataPointVO dpVo = createDisabledPoint(1.0, 0.5, 1, TimePeriods.SECONDS, false, CalculationMode.AVERAGE, 10, TimePeriods.SECONDS, ComparisonMode.GREATER_THAN, 0, TimePeriods.SECONDS);
        //Save some values
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.0, 0), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.1, 1000), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.2, 2000), null);
        dao.savePointValueSync(dpVo.getId(), new PointValueTime(0.3, 3000), null);
        
        timer.fastForwardTo(10000);
        
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        DataPointRT rt = Common.runtimeManager.getDataPoint(dpVo.getId());
        timer.fastForwardTo(10999);
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());

        ensureSetPointValue(rt, new PointValueTime(10.3, timer.currentTimeMillis()));
        timer.fastForwardTo(11000);
        //Nothing raised as our RoC was equal at 11s and dropping
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
        
        ensureSetPointValue(rt, new PointValueTime(11.3, timer.currentTimeMillis()));
        timer.fastForwardTo(13000);
        //Nothing raised as our change is only 1 in the last 10s
        assertEquals(0, listener.raised.size());
        assertEquals(0, listener.rtn.size());
        
        ensureSetPointValue(rt, new PointValueTime(21.4, timer.currentTimeMillis()));
        timer.fastForwardTo(13999);
        assertEquals(1, listener.raised.size());
        assertEquals(13000, listener.raised.get(0).getActiveTimestamp());
        assertEquals(0, listener.rtn.size());

        //Allow reset of alarm after 1s and no change
        timer.fastForwardTo(14000);
        
        assertEquals(1, listener.rtn.size());
        assertEquals(14000, listener.rtn.get(0).getRtnTimestamp());
    }
    
    
    //TODO Test no values set, 1 initial value
    //TODO Test no values set, 0 initial values
    
    /**
     * Create a running datapoint with no initial values
     * 
     *  NOTE: The poll period is 1s for the data source
     * 
     * @param change
     * @param resetRange
     * @param rocThresholdPeriods
     * @param rocThresholdPeriodType
     * @param useAbsoluteValue
     * @param calculationMode
     * @param rocDuration
     * @param rocDurationType
     * @param comparisonMode     
     * @param durationPeriods - duration for RoC to match its comparison before event will go active
     * @param durationPeriodType - duration for RoC to match its comparison before event will go active
     * @return
     */
    protected DataPointRT createRunningPoint(double change, Double resetRange, int rocThresholdPeriods, int rocThresholdPeriodType, boolean useAbsoluteValue, CalculationMode calculationMode, int rocDuration, int rocDurationType, ComparisonMode comparisonMode, int durationPeriods, int durationPeriodType) {
        DataPointVO dpVo = createDisabledPoint(change, resetRange, rocThresholdPeriods, rocThresholdPeriodType, useAbsoluteValue, calculationMode, rocDuration, rocDurationType, comparisonMode, durationPeriods, durationPeriodType);
        dpVo.setEnabled(true);
        Common.runtimeManager.saveDataPoint(dpVo);
        return Common.runtimeManager.getDataPoint(dpVo.getId());
    }
    
    /**
     * Create a point in the database that is not running.
     * 
     * NOTE: The poll period is 1s for the data source
     * 
     * @param rocThreshold
     * @param resetThreshold
     * @param rocThresholdPeriods
     * @param rocThresholdPeriodType
     * @param useAbsoluteValue
     * @param calculationMode
     * @param rocDuration
     * @param rocDurationType
     * @param comparisonMode
     * @param durationPeriods
     * @param durationPeriodType
     * @return
     */
    protected DataPointVO createDisabledPoint(double rocThreshold, Double resetThreshold, int rocThresholdPeriods, int rocThresholdPeriodType, boolean useAbsoluteValue, CalculationMode calculationMode, int rocDuration, int rocDurationType, ComparisonMode comparisonMode, int durationPeriods, int durationPeriodType) {
        MockDataSourceVO dsVo = new MockDataSourceVO("test", "DS_1");
        dsVo.setEnabled(true);
        dsVo.setUpdatePeriods(1);
        dsVo.setUpdatePeriodType(TimePeriods.SECONDS);
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
        rocVo.setRateOfChangeThresholdPeriods(rocThresholdPeriods);
        rocVo.setRateOfChangeThresholdPeriodType(rocThresholdPeriodType);
        if(resetThreshold != null) {
            rocVo.setUseResetThreshold(true);
            rocVo.setResetThreshold(resetThreshold);
        }
        rocVo.setRateOfChangePeriods(rocDuration);
        rocVo.setRateOfChangePeriodType(rocDurationType);
        rocVo.setComparisonMode(comparisonMode);
        rocVo.setCalculationMode(calculationMode);
        rocVo.setUseAbsoluteValue(useAbsoluteValue);
        rocVo.setDuration(durationPeriods);
        rocVo.setDurationType(durationPeriodType);
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
        int retries = 20;
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
