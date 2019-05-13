/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataSource.MockPointLocatorRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

/**
 * @author Terry Packer
 *
 */
public class PointValueCacheTest extends MangoTestBase {

    private int dataSourceId = Common.NEW_ID;
    private int dataPointId = Common.NEW_ID;
    private double currentValue;
    private MockDataSourceVO dsVo;
    private DataPointVO dpVo;
    private MockPointLocatorVO plVo;

    
    //TODO Test RT saves callback does prune list properly
    //TODO Test synchronization
    //TODO Test public void reset(long before)
    //TODO Test public PointValueTime getLatestPointValue() 
    
    @Test
    public void testSaveCallbackPrune() {
        //Insert some test data
        List<PointValueTime> values = insertValues(5);

        //There should only be 5 values
        dpVo.setDefaultCacheSize(10);
        MockPointLocatorRT plRt = new MockPointLocatorRT(plVo);
        DataPointRT rt = new DataPointRT(dpVo, plRt, dsVo, null, timer);
        rt.initialize();

        List<PointValueTime> latest = rt.getLatestPointValues(5);
        assertEquals(5, latest.size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<5; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Insert another 5 values
        for(int i=0; i<5; i++) {
            rt.updatePointValue(new PointValueTime(currentValue, timer.currentTimeMillis()));
            timer.fastForwardTo(timer.currentTimeMillis() + 1000);
            currentValue += 1.0d;
        }
        
        latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
    }
    
    @Test
    public void testCacheReset() {
        //Insert some test data
        List<PointValueTime> values = insertValues(5);

        //There should only be 5 values
        PointValueCache cache = new PointValueCache(dataPointId, 10, null);
        assertEquals(5, cache.getCacheContents().size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<5; i++) {
            assertEquals(values.get(i), cache.getCacheContents().get(cache.getCacheContents().size() - (i + 1)));
        }
        
        //Insert another 5 values
        values.addAll(insertValues(5));
        
        //Expand cache by resetting it
        cache.reset();
        
        List<PointValueTime> latest = cache.getLatestPointValues(10);
        assertEquals(10, cache.getCacheContents().size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.getCacheContents().get(cache.getCacheContents().size() - (i + 1)));
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
    }
    
    
    @Test
    public void testCacheGetLatestPointValues() {
        //Insert some test data
        List<PointValueTime> values = insertValues(5);

        //There should only be 5 values
        PointValueCache cache = new PointValueCache(dataPointId, 10, null);
        assertEquals(5, cache.getCacheContents().size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<5; i++) {
            assertEquals(values.get(i), cache.getCacheContents().get(cache.getCacheContents().size() - (i + 1)));
        }
        
        //Insert another 5 values
        values.addAll(insertValues(5));
        
        //TODO This revealed a bug?  The cache is 5 and won't expand to 10...
        //Expand cache
        List<PointValueTime> latest = cache.getLatestPointValues(10);
        assertEquals(10, cache.getCacheContents().size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.getCacheContents().get(cache.getCacheContents().size() - (i + 1)));
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
    }
    
    @Before
    public void beforePointValueCacheTest() {
        //Create data source
        dsVo = new MockDataSourceVO();
        dsVo.setXid("DS_MOCK_ME");
        dsVo.setName("data source");
        validate(dsVo);
        DataSourceDao.getInstance().save(dsVo);
        dataSourceId = dsVo.getId();
        
        //Create a data point
        plVo = new MockPointLocatorVO(DataTypes.NUMERIC, true);
        dpVo = new DataPointVO();
        dpVo.setXid("DP_MOCK_ME");
        dpVo.setName("point");
        dpVo.setPointLocator(plVo);
        dpVo.setDataSourceId(dataSourceId);
        validate(dpVo);
        DataPointDao.getInstance().save(dpVo);
        dataPointId = dpVo.getId();
        
        currentValue = 0;
    }
    
    @After
    public void afterPointValueCacheTest() {
        if(dataPointId != Common.NEW_ID)
            DataPointDao.getInstance().delete(dataPointId);
        if(dataSourceId != Common.NEW_ID)
            DataSourceDao.getInstance().delete(dataSourceId);
    }
    
    private List<PointValueTime> insertValues(int count) {
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        List<PointValueTime> values = new ArrayList<>();
        for(int i=0; i<count; i++) {
            PointValueTime pvt = new PointValueTime(currentValue, timer.currentTimeMillis());
            dao.savePointValueSync(dataPointId, pvt, null);
            values.add(pvt);
            timer.fastForwardTo(timer.currentTimeMillis() + 1000);
            currentValue += 1.0d;
        }
        return values;
    }
}
