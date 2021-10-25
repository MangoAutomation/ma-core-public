/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.util.usage.DataSourceUsageStatistics;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

import static org.junit.Assert.assertEquals;

public class DataSourceDaoTest extends AbstractVoDaoTest<DataSourceVO, DataSourceDao> {

    @Test
    public void testUsage() {
        for (int i = 0; i < 5; i++) {
            createMockDataSource();
        }
        List<DataSourceUsageStatistics> stats = dao.getUsage();
        assertEquals(1, stats.size());

        DataSourceUsageStatistics stat = stats.get(0);
        assertEquals( (Integer) 5, stat.getCount());
        assertEquals(MockDataSourceDefinition.TYPE_NAME, stat.getDataSourceType());
    }

    @Override
    DataSourceDao getDao() {
        return Common.getBean(DataSourceDao.class);
    }

    @Override
    DataSourceVO newVO() {
        MockDataSourceVO vo = new MockDataSourceVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        //TODO Set all fields
        return vo;
    }

    @Override
    DataSourceVO updateVO(DataSourceVO toUpdate) {
        DataSourceVO copy = (DataSourceVO) toUpdate.copy();
        copy.setName("new name");
        //TODO Update all fields
        return copy;
    }

    @Override
    void assertVoEqual(DataSourceVO expected, DataSourceVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDefinition().getDataSourceTypeName(), actual.getDefinition().getDataSourceTypeName());

        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getEditPermission(), actual.getEditPermission());
        //TODO Flesh out all fields
    }
}
