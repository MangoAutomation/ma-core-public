/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.db.tables.records.PublishedPointsRecord;
import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.util.JsonSerializableUtility;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

public class PublishedPointServiceTest extends AbstractVOServiceTest <PublishedPointVO, PublishedPointsRecord, PublishedPoints, PublishedPointDao, PublishedPointService> {

    private PublisherService publisherService;
    private DataPointService dataPointService;

    @Override
    public void before() {
        super.before();
        publisherService = Common.getBean(PublisherService.class);
        dataPointService = Common.getBean(DataPointService.class);
    }


    @After
    @Override
    public void after() {
        //Since we cannot restart the RuntimeManager, yet we need to clean out the runtime
        publisherService.list().forEach(p -> publisherService.delete(p));
        super.after();
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
        //Start out with a running publisher and an enabled point
        IDataPoint dp = createMockDataPoints(1).get(0);
        MockPublisherVO publisher = createMockPublisher(true);
        MockPublishedPointVO pp = publisher.getDefinition().createPublishedPointVO(publisher, dp);
        pp.setName(dp.getName());
        pp.setEnabled(true);

        PublishedPointVO vo = service.insert(pp);

        assertTrue(service.setPublishedPointState(vo.getXid(), false, false));
        //Ensure it is not running
        assertNull(getRuntimeManager().getPublishedPoint(vo.getId()));

        assertTrue(service.setPublishedPointState(vo.getXid(), true, false));
        //Ensure it is running
        assertNotNull(getRuntimeManager().getPublishedPoint(vo.getId()));
    }

    @Test(expected = NotFoundException.class)
    public void testDeletePoint() {
        IDataPoint dp = createMockDataPoints(1).get(0);
        MockPublisherVO publisher = createMockPublisher(true);
        MockPublishedPointVO pp = publisher.getDefinition().createPublishedPointVO(publisher, dp);
        pp.setName(dp.getName());
        pp.setEnabled(true);

        PublishedPointVO vo = service.insert(pp);
        //Ensure it is running
        assertNotNull(getRuntimeManager().getPublishedPoint(vo.getId()));

        DataPointVO point = dataPointService.get(dp.getId());
        //Delete datapoint
        dataPointService.delete(point);

        //Ensure published point deleted
        service.get(pp.getId());
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        return new MockMangoLifecycle(modules) {
            @Override
            protected RuntimeManager getRuntimeManager() {
                return PublishedPointServiceTest.this.getRuntimeManager();
            }
        };
    }

    RuntimeManager getRuntimeManager() {
        return Common.getBean(RuntimeManager.class);
    }

    @Test
    public void testEmport() {
        //Create publisher and publishedPoints
        MockPublisherVO publisher = createMockPublisher(false);
        List<IDataPoint> dps = createMockDataPoints(5);
        List<PublishedPointVO> pps = new ArrayList<>();
        for (IDataPoint dp : dps) {
            MockPublishedPointVO pp = publisher.getDefinition().createPublishedPointVO(publisher, dp);
            pp.setName(dp.getName());
            pp.setEnabled(true);
            pps.add(service.insert(pp));
        }

        //Export publishedPoints
        String [] exportElements = new String[]{"publishedPoints"};
        Map<String,Object> data = ConfigurationExportData.createExportDataMap(exportElements);
        JsonObject json = null;
        try {
            json = JsonSerializableUtility.convertMapToJsonObject(data);
        } catch (JsonException e) {
            fail(e.getMessage());
        }

        //Ensure publishedPoints
        assertEquals(5, json.getJsonArray("publishedPoints").size());

        //Import publishedPoints for update
        loadConfiguration(json);
        assertEquals(5, service.count());

        //Remove publishedPoints
        pps.forEach(p -> service.delete(p));
        assertEquals(0, service.count());

        //Import publishedPoints for create
        loadConfiguration(json);
        assertEquals(5, service.count());
    }

    @Test
    public void testEmportWithPublisher() {
        //Create publisher without publishedPoints
        createMockPublisher(false);

        //Export publishers
        String [] exportElements = new String[]{"publishers"};
        Map<String,Object> data = ConfigurationExportData.createExportDataMap(exportElements);
        JsonObject json = null;
        try {
            json = JsonSerializableUtility.convertMapToJsonObject(data);
        } catch (JsonException e) {
            fail(e.getMessage());
        }

        //Ensure publisher
        JsonArray publishers = json.getJsonArray("publishers");
        assertEquals(1, publishers.size());

        //Ensure no publishedPoints
        assertEquals(0, service.count());

        //Generate legacy import format
        List<IDataPoint> dps = createMockDataPoints(5);
        JsonArray legacyPoints = new JsonArray();
        for (IDataPoint dp : dps) {
            Map<String, Object> map = new HashMap<>();
            map.put("dataPointId", dp.getXid());
            try {
                JsonObject item = JsonSerializableUtility.convertMapToJsonObject(map);
                legacyPoints.add(item);
            } catch (JsonException e) {
                fail(e.getMessage());
            }
        }
        JsonObject publisherNode = publishers.getJsonObject(0);
        publisherNode.put("points", legacyPoints);

        //Import publishedPoints for create (insert)
        loadConfiguration(json);
        assertEquals(5, service.count());

        //Check that legacy points got imported as enabled
        for (PublishedPointVO pubLegacyPoint : service.list()) {
            assertTrue(pubLegacyPoint.isEnabled());
        }

        //Import publishedPoints for update (duplicate points).  This process
        // always inserts new points and can't update them since there is no XID field
        // on the import data's points array as it was not supported
        loadConfiguration(json);
        assertEquals(10, service.count());

        //Check that legacy points got imported as enabled
        for (PublishedPointVO pubLegacyPoint : service.list()) {
            assertTrue(pubLegacyPoint.isEnabled());
        }
    }
}
