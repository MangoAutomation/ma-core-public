/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.DatabaseProxyFactory;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 *
 * @author Terry Packer
 */
public class PointValueCacheTest extends MangoTestBase {

    /**
     * Test with null cache
     */
    @Test
    public void test0() {
        DataPointVO vo = new DataPointVO();
        vo.setId(1);
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        PointValueCacheDao pointValueCacheDao = Common.databaseProxy.getPointValueCacheDao();
        List<PointValueTime> initialCache = createCache(vo, 5);
        PointValueCache cache = new PointValueCache(vo, 1, null, dao, pointValueCacheDao);

        Assert.assertEquals(initialCache.get(0).getValue(), cache.getLatestPointValue().getValue());
        Assert.assertEquals(initialCache.get(0).getTime(), cache.getLatestPointValue().getTime());
    }

    /**
     * Test an initial cash that is larger than the default
     */
    @Test
    public void test1() {
        DataPointVO vo = new DataPointVO();
        vo.setId(1);
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        PointValueCacheDao pointValueCacheDao = Common.databaseProxy.getPointValueCacheDao();
        List<PointValueTime> initialCache = createCache(vo, 5);
        PointValueCache cache = new PointValueCache(vo, 1, initialCache, dao, pointValueCacheDao);

        Assert.assertEquals(initialCache.get(0).getValue(), cache.getLatestPointValue().getValue());
        Assert.assertEquals(initialCache.get(0).getTime(), cache.getLatestPointValue().getTime());
    }

    /**
     * Test expanding the cache
     */
    @Test
    public void test2() {
        DataPointVO vo = new DataPointVO();
        vo.setId(1);
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        PointValueCacheDao pointValueCacheDao = Common.databaseProxy.getPointValueCacheDao();
        List<PointValueTime> initialCache = createCache(vo, 5);
        PointValueCache cache = new PointValueCache(vo, 1, initialCache.subList(0, 1), dao, pointValueCacheDao);

        List<PointValueTime> latest = cache.getLatestPointValues(5);
        for(int i=0; i<5; i++) {
            Assert.assertEquals(initialCache.get(i).getValue(), latest.get(i).getValue());
            Assert.assertEquals(initialCache.get(i).getTime(), latest.get(i).getTime());
        }

        Assert.assertEquals(5, cache.getCacheContents().size());
    }

    /**
     * Test resetting the cache
     */
    @Test
    public void test3() {
        DataPointVO vo = new DataPointVO();
        vo.setId(1);
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        PointValueCacheDao pointValueCacheDao = Common.databaseProxy.getPointValueCacheDao();
        List<PointValueTime> initialCache = createCache(vo, 5);
        PointValueCache cache = new PointValueCache(vo, 5, initialCache, dao, pointValueCacheDao);
        cache.invalidate(true);

        List<PointValueTime> latest = cache.getLatestPointValues(5);
        for(int i=0; i<5; i++) {
            Assert.assertEquals(initialCache.get(i).getValue(), latest.get(i).getValue());
            Assert.assertEquals(initialCache.get(i).getTime(), latest.get(i).getTime());
        }
    }

    /**
     * Test saving into cache
     */
    @Test
    public void test4() {
        DataPointVO vo = new DataPointVO();
        vo.setId(1);
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        PointValueCacheDao pointValueCacheDao = Common.databaseProxy.getPointValueCacheDao();
        List<PointValueTime> initialCache = createCache(vo, 5);
        PointValueCache cache = new PointValueCache(vo, 5, initialCache, dao, pointValueCacheDao);

        List<PointValueTime> latest = cache.getLatestPointValues(5);
        for(int i=0; i<5; i++) {
            Assert.assertEquals(initialCache.get(i).getValue(), latest.get(i).getValue());
            Assert.assertEquals(initialCache.get(i).getTime(), latest.get(i).getTime());
        }

        List<PointValueTime> newLatest = new ArrayList<>();
        //Save into cache
        for(int i=0; i<10; i++) {
            long time = Common.timer.currentTimeMillis();
            PointValueTime pvt = new PointValueTime((double)(5 + i), time);
            cache.savePointValue(pvt, null, true, true);
            newLatest.add(pvt);
            this.timer.fastForwardTo(this.timer.currentTimeMillis() + 1);
        }

        newLatest.sort((v1,v2) -> {
            return (int)(v2.getTime() - v1.getTime());
        });

        //Check size
        Assert.assertEquals(5, cache.getCacheContents().size());

        List<PointValueTime> latestCached = cache.getLatestPointValues(5);
        for(int i=0; i<5; i++) {
            Assert.assertEquals(newLatest.get(i).getValue(), latestCached.get(i).getValue());
            Assert.assertEquals(newLatest.get(i).getTime(), latestCached.get(i).getTime());
        }

    }

    //TEST RESET
    //Test cache size expansion in multiple threads?

    private List<PointValueTime> createCache(DataPointVO vo, int limit) {
        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        for(int i=0; i<limit; i++) {
            long time = Common.timer.currentTimeMillis();
            this.timer.fastForwardTo(this.timer.currentTimeMillis() + 1);
            dao.savePointValueSync(vo, new PointValueTime((double)i, time), null);
        }
        return dao.getLatestPointValues(vo, limit);
    }


    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setDatabaseProxyFactory(new DatabaseProxyFactory() {
            @Override
            public DatabaseProxy createDatabaseProxy(DatabaseType type) {
                return new DataPointRtMockDatabaseProxy();
            }
        });
        return lifecycle;
    }

    class DataPointRtMockDatabaseProxy extends H2InMemoryDatabaseProxy {
        @Override
        public PointValueDao newPointValueDao() {
            return new MockPointValueDao();
        }
    }

    private List<PointValueTime> values = new ArrayList<PointValueTime>();

    class MockPointValueDao extends PointValueDaoSQL {

        @Override
        public PointValueTime getLatestPointValue(DataPointVO vo) {
            return values.get(values.size() - 1);
        }

        @Override
        public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit) {
            List<PointValueTime> result = new ArrayList<>(values);
            result.sort((v1,v2) -> {
                return (int)(v2.getTime() - v1.getTime());
            });
            return result.subList(0, limit);
        }

        @Override
        public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue,
                SetPointSource source) {
            values.add(pointValue);
            return pointValue;
        }

        @Override
        public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue,
                SetPointSource source) {
            values.add(pointValue);
        }

        @Override
        public void getLatestPointValues(List<DataPointVO> vos, long before, boolean orderById,
                Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
            List<PointValueTime> result = new ArrayList<>(values);
            result.sort((v1,v2) -> {
                return (int)(v2.getTime() - v1.getTime());
            });
            int limitCount = 0;
            for(PointValueTime pvt : result) {
                if(pvt.getTime() < before) {
                    callback.row(new IdPointValueTime(1, pvt.getValue(), pvt.getTime()), limitCount++);
                }
                if(limitCount == limit) {
                    return;
                }
            }
        }

    }

}
