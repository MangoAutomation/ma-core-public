/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.context.annotation.Bean;

import com.infiniteautomation.mango.pointvaluecache.PointValueCache;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.BasicSQLPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataSource.MockDataSourceRT;
import com.serotonin.m2m2.rt.dataSource.MockPointLocatorRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.timer.SimulationTimer;

/**
 * Interval Log On Change works by logging values on an interval and also
 *  allowing changes beyond a tolerance to be logged.  If a change is logged
 *  the interval is restarted from that point.
 *
 * @author Terry Packer
 */
public class DataPointRTTest extends MangoTestBase {

    /**
     * Test Interval Logged Values w/ On Change Option
     *
     */
    @Test
    public void testIntervalOnChangeLogging() {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        MockDataSourceRT dataSource = dsVo.createDataSourceRT();
        dataSource.initialize(false);

        PointValueDao dao = Common.getBean(PointValueDao.class);
        MockPointLocatorVO plVo = new MockPointLocatorVO(DataTypes.NUMERIC, true);
        DataPointVO dpVo = new DataPointVO();
        dpVo.setId(1);
        dpVo.setDataSourceId(dsVo.getId());

        //Configure Interval on change logging
        dpVo.setLoggingType(DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL);
        dpVo.setTolerance(0.5);
        dpVo.setIntervalLoggingPeriod(5);
        dpVo.setIntervalLoggingPeriodType(TimePeriods.SECONDS);

        dpVo.setPointLocator(plVo);
        dpVo.setLoggingType(DataPointVO.LoggingTypes.ON_CHANGE_INTERVAL);

        MockPointLocatorRT plRt = new MockPointLocatorRT(plVo);

        //Setup some initial data
        List<PointValueTime> initialCache = new ArrayList<>();
        initialCache.add(new PointValueTime(1.0, 0));

        SimulationTimer timer = new SimulationTimer();
        DataPointWithEventDetectors dp = new DataPointWithEventDetectors(dpVo, new ArrayList<>());
        DataPointRT rt = new DataPointRT(dp, plRt, dataSource, initialCache, dao, Common.getBean(PointValueCache.class), timer);
        rt.initialize(false);

        //Test no changes
        timer.fastForwardTo(5001);

        PointValueTime value = dao.getLatestPointValue(dpVo);

        //Ensure database has interval logged value
        assertEquals(1.0, value.getDoubleValue(), 0.0001);
        assertEquals(5000, value.getTime());

        //Ensure cache does not have interval logged value
        assertEquals(1.0, rt.getPointValue().getDoubleValue(), 0.0001);
        assertEquals(0, rt.getPointValue().getTime());

        //Next interval
        timer.fastForwardTo(6000);
        rt.setPointValue(new PointValueTime(2.0, 6000), null);

        //Check Log On Change
        value = dao.getLatestPointValue(dpVo);
        assertEquals(2.0, value.getDoubleValue(), 0.0001);
        assertEquals(6000, value.getTime());

        assertEquals(2.0, rt.getPointValue().getDoubleValue(), 0.0001);
        assertEquals(6000, rt.getPointValue().getTime());

        //Interval is reset for 5000ms from now
        timer.fastForwardTo(11001);
        //Check Interval Log
        value = dao.getLatestPointValue(dpVo);
        assertEquals(2.0, value.getDoubleValue(), 0.0001);
        assertEquals(11000, value.getTime());

        assertEquals(2.0, rt.getPointValue().getDoubleValue(), 0.0001);
        assertEquals(6000, rt.getPointValue().getTime());

        //Test Tolerance (Should not get logged)
        timer.fastForwardTo(12000);
        rt.setPointValue(new PointValueTime(2.20, 12000), null);

        //Check Log On Change
        value = dao.getLatestPointValue(dpVo);
        assertEquals(2.0, value.getDoubleValue(), 0.0001);
        assertEquals(11000, value.getTime());

        //Cache will have the set value
        assertEquals(2.2, rt.getPointValue().getDoubleValue(), 0.0001);
        assertEquals(12000, rt.getPointValue().getTime());
    }

    //TODO Test for Historical Generation
    //TODO Test Quantized

    public static class DataPointRTTestConfig {
        @Bean
        public PointValueDao pointValueDao(DatabaseProxy databaseProxy) {
            return new MockPointValueDao(databaseProxy);
        }
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.addRuntimeContextConfiguration(DataPointRTTestConfig.class);
        return lifecycle;
    }


    static class MockPointValueDao extends BasicSQLPointValueDao {

        private final List<PointValueTime> values = new ArrayList<>();

        public MockPointValueDao(DatabaseProxy databaseProxy) {
            super(databaseProxy);
        }

        @Override
        public PointValueTime getLatestPointValue(DataPointVO vo) {
            return values.get(values.size() - 1);
        }

        @Override
        public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue,
                SetPointSource source) {
            values.add(pointValue);
        }

    }

}
