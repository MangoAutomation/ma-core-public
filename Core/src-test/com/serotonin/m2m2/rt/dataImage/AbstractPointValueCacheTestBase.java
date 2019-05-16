/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

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
 * Base class for testing the point value cache
 * @author Terry Packer
 *
 */
public class AbstractPointValueCacheTestBase extends MangoTestBase {

    protected int dataSourceId = Common.NEW_ID;
    protected int dataPointId = Common.NEW_ID;
    protected double currentValue;
    protected MockDataSourceVO dsVo;
    protected DataPointVO dpVo;
    protected MockPointLocatorVO plVo;
    protected MockPointLocatorRT plRt;
    protected DataPointRT rt;
    
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
    protected void setupRuntime(int cacheSize) {
        dpVo.setDefaultCacheSize(cacheSize);
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
     * Wait for values to be available in the database, wait retries x 100 ms
     * @param count
     * @param retries
     * @throws InterruptedException
     */
    protected void ensureValuesInDatabase(int count, int retries) {
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        int trial = retries;
        while(trial >= 0) {
            if(dao.getLatestPointValues(dataPointId, count).size() >= count)
                return;
            try{ Thread.sleep(100); }catch(InterruptedException e) { }
            trial--;
        }
        if(trial == 0)
            fail("Not enough values in database.");
    }
    
    /**
     * Insert values directly into the DataPointRT and its cache.  Used to simulate a running point
     * saving values via the BWB.  Expect a delay between this call returning and values being 
     * query-able in the database.
     * @param count
     * @return
     */
    protected List<PointValueTime> insertValuesIntoRuntime(int count) {
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
    protected List<PointValueTime> insertValuesIntoDatabase(int count) {
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
