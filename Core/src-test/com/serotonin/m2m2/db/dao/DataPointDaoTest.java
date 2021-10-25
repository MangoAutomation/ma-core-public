/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.util.AssertionUtils;
import com.infiniteautomation.mango.util.usage.DataPointUsageStatistics;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

import static org.junit.Assert.*;

public class DataPointDaoTest extends AbstractVoDaoTest<DataPointVO, DataPointDao> implements AssertionUtils {

    private DataSourceService dataSourceService;
    private PointValueDaoSQL pointValueDaoSQL;

    @Override
    public void before() {
        super.before();
        dataSourceService = Common.getBean(DataSourceService.class);
        pointValueDaoSQL = Common.getBean(PointValueDaoSQL.class);
    }

    @Test
    public void testIsEnabled() {
        DataPointVO point = newVO();
        dao.insert(point);
        boolean enabled = dao.isEnabled(point.getId());
        assertFalse(enabled);

        point.setEnabled(true);
        dao.update(point.getId(), point);
        enabled = dao.isEnabled(point.getId());
        assertTrue(enabled);
    }

    @Test
    public void testCountPointsForDataSourceType() {
        createMockDataPoints(5);
        int pointCount = dao.countPointsForDataSourceType(MockDataSourceDefinition.TYPE_NAME);
        assertEquals(5, pointCount);
    }

    @Test
    public void testTopPointHistoryCountsSql() {
        DataPointVO point = newVO();
        dao.insert(point);
        for (int i = 0; i < 5; i++) {
            PointValueTime newPvt = new PointValueTime(i, timer.currentTimeMillis());
            pointValueDaoSQL.savePointValueSync(point, newPvt, null);
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
        }
        List<PointHistoryCount> historyCounts = dao.getTopPointHistoryCounts();
        assertEquals(1, historyCounts.size());

        DataPointVO fromDB = dao.get(point.getId());
        PointHistoryCount history = historyCounts.get(0);
        assertEquals(fromDB.getId(), history.getPointId());
        assertEquals(fromDB.getExtendedName(), history.getPointName());
        assertEquals(5, history.getCount());
    }

    @Test
    public void testUsage() {
        for (int i = 0; i < 5; i++) {
            DataPointVO point = newVO();
            dao.insert(point);
        }
        List<DataPointUsageStatistics> stats = dao.getUsage();
        assertEquals(1, stats.size());

        DataPointUsageStatistics stat = stats.get(0);
        assertEquals((Integer) 5, stat.getCount());
        assertEquals(MockDataSourceDefinition.TYPE_NAME, stat.getDataSourceType());
    }

    @Override
    void assertVoEqual(DataPointVO expected, DataPointVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());

        assertEquals(expected.getDataSourceId(), actual.getDataSourceId());
        assertEquals(expected.getPointLocator().getDataTypeId(), actual.getPointLocator().getDataTypeId());

        assertEquals(expected.getSeriesId(), actual.getSeriesId());

        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getEditPermission(), actual.getEditPermission());
        assertPermission(expected.getSetPermission(), actual.getSetPermission());
    }

    @Override
    DataPointVO newVO() {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        dsVo.setName("permissions_test_datasource");
        DataSourceVO mock = dataSourceService.insert(dsVo);

        //Create the point
        DataPointVO vo = new DataPointVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        vo.setDataSourceId(mock.getId());
        vo.setPointLocator(new MockPointLocatorVO());
        //TODO Set all fields

        return vo;
    }

    @Override
    DataPointVO updateVO(DataPointVO existing) {
        DataPointVO copy = existing.copy();
        //TODO Modify all fields
        return copy;
    }

    @Override
    DataPointDao getDao() {
        return Common.getBean(DataPointDao.class);
    }
}
