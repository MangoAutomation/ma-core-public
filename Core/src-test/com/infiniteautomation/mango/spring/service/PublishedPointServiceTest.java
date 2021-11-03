/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.db.tables.records.PublishedPointsRecord;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

public class PublishedPointServiceTest extends AbstractVOServiceTest <PublishedPointVO, PublishedPointsRecord, PublishedPoints, PublishedPointDao, PublishedPointService> {

    private PublisherService publisherService;
    private DataPointService dataPointService;
    private DataSourceService dataSourceService;

    @Override
    public void before() {
        super.before();
        publisherService = Common.getBean(PublisherService.class);
        dataPointService = Common.getBean(DataPointService.class);
        dataSourceService = Common.getBean(DataSourceService.class);
    }

    @Override
    PublishedPointService getService() {
        return Common.getBean(PublishedPointService.class);
    }

    @Override
    PublishedPointDao getDao() {
        return Common.getBean(PublishedPointDao.class);
    }

    @Override
    void assertVoEqual(PublishedPointVO expected, PublishedPointVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getPublisherId(), actual.getPublisherId());
        assertEquals(expected.getDataPointId(), actual.getDataPointId());
        assertEquals(expected.getDataPointXid(), actual.getDataPointXid());
        assertEquals(expected.getJsonData(), actual.getJsonData());
        assertEquals(expected.getPublisherTypeName(), actual.getPublisherTypeName());
        if(expected instanceof MockPublishedPointVO) {
            //Assert test field to test db serialization
            assertEquals(((MockPublishedPointVO)expected).getTestField(), ((MockPublishedPointVO)actual).getTestField());
        }
    }

    @Override
    PublishedPointVO newVO(User owner) {
        IDataPoint dp = createMockDataPoints(1).get(0);
        MockPublisherVO publisher = createMockPublisher(false);
        MockPublishedPointVO pp = publisher.getDefinition().createPublishedPointVO(publisher, dp);
        pp.setName(dp.getName());
        pp.setEnabled(true);

        return pp;
    }

    @Override
    PublishedPointVO updateVO(PublishedPointVO existing) {
        MockPublishedPointVO vo = (MockPublishedPointVO)existing;
        vo.setTestField("Testing");
        return vo;
    }

    @Test(expected = PermissionException.class)
    public void testNonAdminCreate() {
        runAs.runAs(readUser, () -> {
            insertNewVO(readUser);
        });
    }

    @Test(expected = PermissionException.class)
    public void testNonAdminUpdate() {
        PublishedPointVO vo = insertNewVO(readUser);
        PublishedPointVO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        PublishedPointVO updated = updateVO(vo);
        runAs.runAs(readUser, () -> {
            service.update(vo.getId(), updated);
        });
    }

    @Test(expected = PermissionException.class)
    public void testNonAdminDelete() {
        PublishedPointVO vo = insertNewVO(readUser);
        PublishedPointVO fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        runAs.runAs(readUser, () -> {
            service.delete(vo);
        });
    }

    @Test
    public void testSetPublishedPointState() {
        PublishedPointVO vo = insertNewVO(readUser);
        assertTrue(service.setPublishedPointState(vo.getXid(), false, false));
        assertTrue(service.setPublishedPointState(vo.getXid(), true, false));
    }

    @Test
    public void testReplacePoints() {
        MockPublisherVO publisher = createMockPublisher(false);
        List<IDataPoint> dps = createMockDataPoints(5);
        List<PublishedPointVO> pps = new ArrayList<>();
        for (IDataPoint dp : dps) {
            MockPublishedPointVO pp = publisher.getDefinition().createPublishedPointVO(publisher, dp);
            pp.setName(dp.getName());
            pp.setXid(UUID.randomUUID().toString());
            pp.setEnabled(true);
            pps.add(pp);
        }
        service.replacePoints(publisher.getId(), pps);
        List<PublishedPointVO> actualPoints =service.getPublishedPoints(publisher.getId());
        assertEquals(pps.size(), actualPoints.size());
        for (int i = 0; i < actualPoints.size(); i++) {
            assertVoEqual(pps.get(i), actualPoints.get(i));
        }
    }
}
