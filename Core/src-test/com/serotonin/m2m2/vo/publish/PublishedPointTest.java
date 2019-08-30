/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

/**
 * @author Terry Packer
 *
 */
public class PublishedPointTest extends MangoTestBase {
    
    @BeforeClass
    public static void addDefinitions() {
        List<ModuleElementDefinition> definitions = new ArrayList<>();
        definitions.add(new MockPublisherDefinition());
        addModule("mockPublisher", definitions);
    }
    
    @Test
    public void testPublisherAuditTrail() {
        MockPublisherVO pub = new MockPublisherVO();
        pub.setDefinition(new MockPublisherDefinition());
        pub.setXid("PUB_TEST1");
        pub.setEnabled(false);
        PublisherDao.getInstance().savePublisher(pub);
        
        MockDataSourceVO ds = new MockDataSourceVO();
        ds.setXid("DS_TEST1");
        ds.setName("TEST");
        DataSourceDao.getInstance().save(ds);
        
        //Save a point
        DataPointVO dp = new DataPointVO();
        dp.setXid("DP_1");
        dp.setName("Data point name");
        dp.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));
        dp.setDataSourceId(ds.getId());
        DataPointDao.getInstance().save(dp);
        
        //Save publisher point
        MockPublishedPointVO pp = new MockPublishedPointVO();
        pp.setXid(PublishedPointDao.getInstance().generateUniqueXid());
        pp.setName("Test published point");
        pp.setPublisherId(pub.getId());
        pp.setDataPointId(dp.getId());
        pp.setMockSetting("mock setting");
        pp.ensureValid();
        PublishedPointDao.getInstance().save(pp);
        
        //Verify the point was created correctly
        MockPublishedPointVO mpp = (MockPublishedPointVO)PublishedPointDao.getInstance().get(pp.getId());
        Assert.assertEquals(pp.getId(), mpp.getId());
        Assert.assertEquals(pp.getXid(), mpp.getXid());
        Assert.assertEquals(pp.getName(), mpp.getName());
        Assert.assertEquals(pp.isEnabled(), mpp.isEnabled());
        Assert.assertEquals(pp.getPublisherId(), mpp.getPublisherId());
        Assert.assertEquals(pp.getDataPointId(), mpp.getDataPointId());
        Assert.assertEquals(pp.getMockSetting(), mpp.getMockSetting());
        
        //TODO Verify Audit Events
    }
}
