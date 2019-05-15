/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    private MockPointLocatorRT plRt;
    private DataPointRT rt;

    //TODO Test synchronization
    //TODO Test public void reset(long before)
    //TODO Test public PointValueTime getLatestPointValue() 
    //TODO Test insert backdated values 
    
    
    @Test
    public void testSaveCallbackPrune() throws InterruptedException {
        setupRuntime();
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        List<PointValueTime> latest = rt.getLatestPointValues(5);
        assertEquals(5, latest.size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<5; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Insert another 5 values
        values.addAll(insertValuesIntoRuntime(5));
        
        latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Insert another 5 to see the cache > 10 and then trim back to 10
        values.addAll(insertValuesIntoRuntime(5));
        
        int retries = 10;
        while(retries > 0) {
            //Force refresh so that all saved values get cleared out
            rt.resetValues();
            List<PointValueTime> cacheCopy = rt.getCacheCopy();
            if(cacheCopy.size() == 10)
                break;
            Thread.sleep(100);
            retries--;
        }
        if(retries == 0)
            fail("Didn't recieve all values into cache.  Cache size is " + rt.getCacheCopy().size());
        
        //assert cache contents, there will have been 15 inserted and we will compare the latest 10 which should be
        // in the cache
        List<PointValueTime> cacheCopy = rt.getCacheCopy();
        assertEquals(10, cacheCopy.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i + 5), cacheCopy.get(cacheCopy.size() - (i + 1)));
        }
    }
    
    @Test
    public void testCacheReset() {
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoDatabase(5);

        setupRuntime();
        
        //There should only be 5 values
        List<PointValueTime> cache = rt.getCacheCopy();
        assertEquals(5, cache.size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<5; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
        }
        
        //Insert another 5 values
        values.addAll(insertValuesIntoRuntime(5));
        
        //Expand cache by resetting it
        rt.resetValues();
        
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
    }
    
    
    @Test
    public void testCacheGetLatestPointValues() {
        
        setupRuntime();
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        //There should only be 5 values
        List<PointValueTime> cache = rt.getCacheCopy();
        assertEquals(5, cache.size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<5; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
        }
        
        //Insert another 5 values
        values.addAll(insertValuesIntoRuntime(5));
        
        //TODO This revealed a bug?  The cache is 5 and won't expand to 10...
        //Expand cache
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
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

    /**
     * Setup the runtime, useful to fill database with values to load in cache on initialization of point
     */
    private void setupRuntime() {
        dpVo.setDefaultCacheSize(10);
        plRt = new MockPointLocatorRT(plVo);
        rt = new DataPointRT(dpVo, plRt, dsVo, null, timer);
        rt.initialize();
    }
    
    @After
    public void afterPointValueCacheTest() {
        if(dataPointId != Common.NEW_ID)
            DataPointDao.getInstance().delete(dataPointId);
        if(dataSourceId != Common.NEW_ID)
            DataSourceDao.getInstance().delete(dataSourceId);
    }
    
    /**
     * Insert values directly into the DataPointRT and its cache.  Used to simulate a running point
     * saving values.
     * @param count
     * @return
     */
    private List<PointValueTime> insertValuesIntoRuntime(int count) {
        List<PointValueTime> values = new ArrayList<>();
        for(int i=0; i<count; i++) {
            PointValueTime pvt = new PointValueTime(currentValue, timer.currentTimeMillis());
            rt.updatePointValue(pvt);
            values.add(pvt);
            timer.fastForwardTo(timer.currentTimeMillis() + 1000);
            currentValue += 1.0d;
        }
        return values;
    }
    
    /**
     * Put the values directly into the database, bypass the cache.  Helps to simulate a point not running
     * and its values being updated externally via a sync or import.
     * @param count
     * @return
     */
    private List<PointValueTime> insertValuesIntoDatabase(int count) {
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
