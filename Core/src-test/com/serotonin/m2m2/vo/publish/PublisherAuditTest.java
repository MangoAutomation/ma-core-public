/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

/**
 *
 * @author Terry Packer
 */
public class PublisherAuditTest extends MangoTestBase {

    private int pointCount = 100;
    
    @BeforeClass
    public static void addDefinitions() {
        List<ModuleElementDefinition> definitions = new ArrayList<>();
        definitions.add(new MockPublisherDefinition());
        addModule("mockPublisher", definitions);
    }
    
    @Test
    public void testPublisherAuditTrail() {
        MockPublisherVO vo = new MockPublisherVO();
        vo.setDefinition(new MockPublisherDefinition());
        vo.setXid("PUB_TEST1");
        vo.setPoints(createPoints());
        vo.setEnabled(false);
        
        PublisherDao.getInstance().savePublisher(vo);
        
        //Modify and re-save
        vo.setEnabled(true);
        PublisherDao.getInstance().savePublisher(vo);
        
        //TODO Verify Audit Events
    }
    
    protected List<MockPublishedPointVO> createPoints(){
        List<MockPublishedPointVO> points = new ArrayList<MockPublishedPointVO>(pointCount);
        
        //Create data source?
        MockDataSourceVO ds = new MockDataSourceVO();
        ds.setXid("DS_TEST1");
        ds.setName("TEST");
        DataSourceDao.getInstance().insert(ds);
        
        for(int i=1; i<pointCount + 1; i++) {
            DataPointVO dp = new DataPointVO();
            dp.setXid("DP_" + i);
            dp.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));
            dp.setDataSourceId(ds.getId());
            DataPointDao.getInstance().insert(dp);
            MockPublishedPointVO vo = new MockPublishedPointVO();
            vo.setDataPointId(i);
            points.add(vo);
        }
        return points;
    }
    
}
