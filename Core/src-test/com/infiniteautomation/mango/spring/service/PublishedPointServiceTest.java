/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.db.tables.records.PublishedPointsRecord;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

import static org.junit.Assert.assertEquals;

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
        assertEquals(expected.getPublisherId(), actual.getId());
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
        //TODO Create data source, publisher and point to publish
        MockPublishedPointVO pp = new MockPublishedPointVO();
        pp.setDataPointId(dp.getId());
        pp.setPublisherId(publisher.getId());
        return pp;
    }

    @Override
    PublishedPointVO updateVO(PublishedPointVO existing) {
        MockPublishedPointVO vo = (MockPublishedPointVO)existing;
        vo.setTestField("Testing");
        return vo;
    }
}
