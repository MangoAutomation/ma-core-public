/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import static org.junit.Assert.fail;

import java.util.List;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;

/**
 * Base class for benchmarking Mango
 */
public class MockMango extends MangoTestBase {

    public List<IDataPoint> createMockDataPoints(int count) {
        return super.createMockDataPoints(count);
    }

    protected List<IDataPoint> createMockDataPointsWithDetectors(int count) {
        List<IDataPoint> points = super.createMockDataPoints(count);
        EventDetectorDefinition<?> updateEventDetectorDefinition = ModuleRegistry.getEventDetectorDefinition("UPDATE");
        DataPointService service = Common.getBean(DataPointService.class);
        DataPointDao dao = Common.getBean(DataPointDao.class);
        EventDetectorsService eventDetectorsService = Common.getBean(EventDetectorsService.class);
        for (IDataPoint point : points) {
            DataPointVO vo = (DataPointVO) point;
            vo.setSeriesId(dao.insertNewTimeSeries());
            try {
                service.update(vo.getId(), vo);
            } catch (ValidationException e) {
                String failureMessage = "";
                for (ProcessMessage m : e.getValidationResult().getMessages()) {
                    String messagePart = m.getContextKey() + " -> " + m.getContextualMessage().translate(Common.getTranslations()) + "\n";
                    failureMessage += messagePart;
                }
                fail(failureMessage);
            }
            updateEventDetectorDefinition.baseCreateEventDetectorVO(point.getId());
            eventDetectorsService.insert(updateEventDetectorDefinition.baseCreateEventDetectorVO(point.getId()));
        }
        return points;
    }


    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifeycle = new MockMangoLifecycle(modules);
        //TODO set database type
        return lifeycle;
    }
}
