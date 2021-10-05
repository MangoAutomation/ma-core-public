/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.dataPoint.DataPointChangeDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.UpdateEventDetectorDefinition;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.UpdateDetectorVO;

/**
 * @author Jared Wiltshire
 */
public class DataPointChangeDefinitionTest extends MangoTestBase {

    private static class TestDefinition extends DataPointChangeDefinition {
        @Autowired
        private EventDetectorsService eventDetectorsService;

        @Override
        public Collection<AbstractPointEventDetectorVO> postInsert(DataPointVO vo) {
            PointEventDetectorDefinition<UpdateDetectorVO> updateDef = ModuleRegistry.getEventDetectorDefinition(UpdateEventDetectorDefinition.TYPE_NAME);
            UpdateDetectorVO detector = updateDef.baseCreateEventDetectorVO(vo);
            detector.setName(vo.getName());
            detector.setAlarmLevel(AlarmLevels.INFORMATION);
            eventDetectorsService.insertAndReload(detector, false);
            return Collections.singleton(detector);
        }
    }

    @BeforeClass
    public static void beforeClass() {
        addModule("dataPointChangeDefinitionTest", new TestDefinition());
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        return new MockMangoLifecycle(modules) {
            /**
             * Returns a real runtime manager implementation instead of mock
             */
            @Override
            protected RuntimeManager getRuntimeManager() {
                return Common.getBean(RuntimeManager.class);
            }
        };
    }

    @Test
    public void addDetectorsOnInsert() {
        MockDataSourceVO ds = createMockDataSource(true);
        DataPointVO point = createMockDataPoint(ds, dp -> dp.setEnabled(true));

        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());
        Assert.assertNotNull(rt);
        List<PointEventDetectorRT<?>> runningDetectors = rt.getEventDetectors();
        Assert.assertEquals(1, runningDetectors.size());
    }

}
