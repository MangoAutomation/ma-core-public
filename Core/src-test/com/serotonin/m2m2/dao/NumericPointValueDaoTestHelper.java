/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Assert;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.infiniteautomation.mango.db.query.QueryCancelledException;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 *
 * Class to keep all the test logic in one place, so we
 * can test the various PointValueDao implementations equally.
 *
 * The test
 *
 * @author Terry Packer
 */
public class NumericPointValueDaoTestHelper {

    protected DataPointVO vo1;
    protected DataPointVO vo2;
    protected DataPointVO emptyDataPointVO;
    protected List<DataPointVO> vos;
    protected Map<Integer, List<PointValueTime>> data;
    protected static long startTs;
    protected static long endTs;
    protected long series2StartTs;
    protected long series2EndTs;
    protected int totalSampleCount;
    protected final PointValueDao dao;

    public NumericPointValueDaoTestHelper(DataPointVO vo1, DataPointVO vo2, DataPointVO empty, PointValueDao dao) {
        this.vo1 = vo1;
        this.vo2 = vo2;
        this.emptyDataPointVO = empty;
        this.dao = dao;
        this.vos = new ArrayList<>();
        this.vos.add(vo1);
        this.vos.add(vo2);
        this.data = new HashMap<>();
    }

    /**
     * Get the points used for this test
     */
    protected List<DataPointVO> getVos() {
        return vos;
    }

    /**
     * Insert some test data.
     * Call before every test.
     */
    public void before() {
        //Start back 30 days
        endTs = System.currentTimeMillis();
        startTs = endTs - (30L * 24L * 60L * 60L * 1000L);

        for(DataPointVO vo : vos) {
            this.data.put(vo.getSeriesId(), new ArrayList<>());
        }

        //Insert a few samples for series 2 before our time
        series2StartTs = startTs - (1000 * 60 * 15);
        long time = series2StartTs;
        PointValueTime p2vt = new PointValueTime(-3.0, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getSeriesId()).add(p2vt);

        time = startTs - (1000 * 60 * 10);
        p2vt = new PointValueTime(-2.0, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getSeriesId()).add(p2vt);

        time = startTs - (1000 * 60 * 5);
        p2vt = new PointValueTime(-1.0, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getSeriesId()).add(p2vt);

        time = startTs;
        //Insert a sample every 5 minutes
        double value = 0.0;
        while(time < endTs){
            PointValueTime pvt = new PointValueTime(value, time);
            this.data.get(vo1.getSeriesId()).add(pvt);
            this.data.get(vo2.getSeriesId()).add(pvt);
            this.dao.savePointValueSync(vo1, pvt, null);
            this.dao.savePointValueSync(vo2, pvt, null);
            time = time + 1000 * 60 * 5;
            totalSampleCount++;
            value++;
        }

        //Add a few more samples for series 2 after our time
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getSeriesId()).add(p2vt);

        time = time + (1000 * 60 * 5);
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getSeriesId()).add(p2vt);

        time = time + (1000 * 60 * 5);
        p2vt = new PointValueTime(value, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getSeriesId()).add(p2vt);
        this.series2EndTs = time;
    }

    public PointValueDao getDao() {
        return dao;
    }

    /**
     * Call after every test
     */
    public void after() {
        this.dao.deleteAllPointDataWithoutCount();
    }

    /* Latest Multiple w/ callback Test Methods */
    public void testLatestExceptionInCallback () {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(endTs);
        try {
            this.dao.getLatestPointValuesCombined(vos, endTs, Integer.MAX_VALUE, new PVTQueryCallback<>() {

                int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
                int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 4; //Start before last 3 samples (extra)
                @Override
                public void row(IdPointValueTime value) {

                    mutableIndex.increment();
                    count.increment();
                    if(value.getTime() > timestamp.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp.setValue(value.getTime());
                    if(value.getSeriesId() == vo2.getSeriesId()) {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                        seriesId2Counter--;
                    }else {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                        seriesIdCounter--;
                    }
                    if(count.getValue() == 20)
                        throw new QueryCancelledException(new Exception("Exception Test"));
                }

            });
        }catch(QueryCancelledException e) {
            // noop
        }
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testLatestNoDataInBothSeries() {
        MutableInt count = new MutableInt();
        this.dao.getLatestPointValuesCombined(vos, series2StartTs, Integer.MAX_VALUE, (value) -> count.increment());
        Assert.assertEquals(Integer.valueOf(0), count.getValue());
    }

    public void testLatestNoDataInOneSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.getLatestPointValuesCombined(vos, startTs, Integer.MAX_VALUE, new PVTQueryCallback<>() {
            int seriesId2Counter = 2;

            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if (value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if (value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                } else {
                    Assert.fail("Should not get data for series 1");
                }
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(3) , count.getValue());
    }

    public void testLatestMultiplePointValuesNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(endTs);
        this.dao.getLatestPointValuesCombined(vos, endTs, Integer.MAX_VALUE, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 4; //Start before last 3 samples (extra)

            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if (value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if (value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                } else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 3) , count.getValue());
    }

    /**
     * Test where point 2 has more data than point 1
     */
    public void testLatestMultiplePointValuesNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2EndTs + 1);
        this.dao.getLatestPointValuesCombined(vos, series2EndTs + 1, Integer.MAX_VALUE, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 1;
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());
    }

    public void testLatestMultiplePointValuesOrderByIdNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(endTs);
        MutableLong timestamp2 = new MutableLong(endTs);
        this.dao.getLatestPointValuesPerPoint(vos, endTs, Integer.MAX_VALUE, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 4; //Start before last 3 samples (extra)
            @Override
            public void row(IdPointValueTime value) {

                count.increment();
                if(mutableIndex.getAndIncrement() < data.get(vo1.getSeriesId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 3) , count.getValue());

    }

    public void testLatestMultiplePointValuesOrderByIdNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2EndTs + 1);
        MutableLong timestamp2 = new MutableLong(series2EndTs + 1);
        this.dao.getLatestPointValuesPerPoint(vos, series2EndTs + 1, Integer.MAX_VALUE, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 1;
            @Override
            public void row(IdPointValueTime value) {

                count.increment();
                if(mutableIndex.getAndIncrement() < data.get(vo1.getSeriesId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());

    }

    public void testLatestMultiplePointValuesLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(endTs);
        this.dao.getLatestPointValuesCombined(vos, endTs,20, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 4;
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count.getValue());
    }

    public void testLatestMultiplePointValuesLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2EndTs + 1);
        this.dao.getLatestPointValuesCombined(vos, series2EndTs + 1, 20, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 1;
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count.getValue());
    }

    public void testLatestMultiplePointValuesOrderByIdLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(endTs);
        MutableLong timestamp2 = new MutableLong(endTs);
        this.dao.getLatestPointValuesPerPoint(vos, endTs,20, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 4;
            @Override
            public void row(IdPointValueTime value) {

                if(mutableIndex.getAndIncrement() < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count1.getValue());
        Assert.assertEquals(Integer.valueOf(20), count2.getValue());
    }

    public void testLatestMultiplePointValuesOrderByIdLimitOffsetSeries() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2EndTs + 1);
        MutableLong timestamp2 = new MutableLong(series2EndTs + 1);
        this.dao.getLatestPointValuesPerPoint(vos, series2EndTs + 1,20, new PVTQueryCallback<>() {

            int seriesIdCounter = data.get(vo1.getSeriesId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getSeriesId()).size() - 1;
            @Override
            public void row(IdPointValueTime value) {

                if(mutableIndex.getAndIncrement() < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count1.getValue());
        Assert.assertEquals(Integer.valueOf(20), count2.getValue());
    }

    /* Values Between Tests */
    public void testBetweenExceptionInCallback() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp  = new MutableLong(startTs);
        try {
            this.dao.getPointValuesBetween(vos, startTs, endTs, false, null, new PVTQueryCallback<>() {

                int seriesIdCounter = 0;
                int seriesId2Counter = 3; //Skip first 3
                @Override
                public void row(IdPointValueTime value) {

                    mutableIndex.increment();
                    count.increment();
                    if(value.getTime() < timestamp.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp.setValue(value.getTime());
                    if(value.getSeriesId() == vo2.getSeriesId()) {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                        seriesId2Counter++;
                    }else {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                        seriesIdCounter++;
                    }
                    if(count.getValue() == 20)
                        throw new QueryCancelledException(new Exception("Exception Test"));
                }

            });
        }catch(QueryCancelledException e) {
            // noop
        }
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testBetweenNoDataInBothSeries() {
        MutableInt count = new MutableInt();
        this.dao.getPointValuesBetween(vos, 0, series2StartTs - 1, false, null, (value) -> count.increment());
        Assert.assertEquals(Integer.valueOf(0), count.getValue());
    }

    public void testBetweenNoDataInOneSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(0);
        this.dao.getPointValuesBetween(vos, 0, startTs, false, null, new PVTQueryCallback<>() {
            int seriesId2Counter = 0;
            @Override
            public void row(IdPointValueTime value) {
                count.increment();

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    Assert.fail("Should not get data for series 1");
                }
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(3) , count.getValue());
    }
    public void testRangeMultiplePointValuesNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.getPointValuesBetween(vos, startTs, endTs, false, null, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
    }

    public void testRangeMultiplePointValuesNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2StartTs);
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs + 1, false, null, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());
    }

    public void testRangeMultiplePointValuesOrderByIdNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(startTs);
        MutableLong timestamp2 = new MutableLong(startTs);
        this.dao.getPointValuesBetween(vos, startTs, endTs, true, null, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value) {

                count.increment();
                if(mutableIndex.getAndIncrement() < data.get(vo1.getSeriesId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
    }

    public void testRangeMultiplePointValuesOrderByIdNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2StartTs);
        MutableLong timestamp2 = new MutableLong(series2StartTs);
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs+1, true, null, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0; //Skip first 3
            @Override
            public void row(IdPointValueTime value) {

                count.increment();
                if(mutableIndex.getAndIncrement() < data.get(vo1.getSeriesId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());
    }

    public void testRangeMultiplePointValuesLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.getPointValuesBetween(vos, startTs, endTs, false, 20, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testRangeMultiplePointValuesLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2StartTs);
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs, false, 20, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testRangeMultiplePointValuesOrderByIdLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(startTs);
        MutableLong timestamp2 = new MutableLong(startTs);

        this.dao.getPointValuesBetween(vos, startTs, endTs, true, 20, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value) {

                if(mutableIndex.getAndIncrement() < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count1.getValue());
        Assert.assertEquals(Integer.valueOf(20), count2.getValue());
    }
    public void testRangeMultiplePointValuesOrderByIdLimitOffsetSeries() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2StartTs);
        MutableLong timestamp2 = new MutableLong(series2StartTs);
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs, true, 20, new PVTQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0; //Skip first 3
            @Override
            public void row(IdPointValueTime value) {

                if(mutableIndex.getAndIncrement() < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count1.getValue());
        Assert.assertEquals(Integer.valueOf(20), count2.getValue());
    }

    public void testWideQueryNoData() {
        this.dao.wideQuery(emptyDataPointVO, 0, 100, new WideQueryCallback<>() {

            @Override
            public void preQuery(PointValueTime value) {
                Assert.fail("Should not have data");
            }

            @Override
            public void row(PointValueTime value) {
                Assert.fail("Should not have data");
            }

            @Override
            public void postQuery(PointValueTime value) {
                Assert.fail("Should not have data");
            }
        });
    }

    public void testWideQueryNoBefore() {
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.wideQuery(vo1, startTs - 1, endTs - 1, new WideQueryCallback<>() {
            int counter = 0;
            @Override
            public void preQuery(PointValueTime value) {
                Assert.fail("Should not have data");
            }

            @Override
            public void row(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getSeriesId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void postQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getSeriesId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }
        });
    }

    public void testWideQuery() {
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.wideQuery(vo1, startTs + 1, endTs - 1, new WideQueryCallback<>() {
            int counter = 0;
            @Override
            public void preQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getSeriesId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void row(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getSeriesId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void postQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getSeriesId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }
        });
    }

    public void testWideQueryNoAfter() {
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.wideQuery(vo1, startTs + 1, endTs, new WideQueryCallback<>() {
            int counter = 0;
            @Override
            public void preQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getSeriesId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void row(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getSeriesId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void postQuery(PointValueTime value) {
                Assert.fail("Should not have data");
            }
        });
    }

    /* Bookend Tests */
    public void testBookendExceptionInFirstValueCallback() {

        try {
            this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<>() {
                @Override
                public void firstValue(IdPointValueTime value, boolean bookend) {
                    throw new QueryCancelledException(new Exception("First Value Callback Exception"));
                }

                @Override
                public void row(IdPointValueTime value) {
                    Assert.fail("Query cancelled, should not get any rows");
                }

                @Override
                public void lastValue(IdPointValueTime value, boolean bookend) {
                    Assert.fail("Query cancelled, should not get last value");
                }

            });
        }catch(QueryCancelledException e) {
            // noop
        }
    }

    public void testBookendExceptionInRowCallback() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        try {
            this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<>() {

                int seriesIdCounter = 0;
                int seriesId2Counter = 3; //Skip first 3

                @Override
                public void firstValue(IdPointValueTime value, boolean bookend) {

                    mutableIndex.increment();
                    if(value.getTime() < timestamp.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp.setValue(value.getTime());
                    if(value.getSeriesId() == vo2.getSeriesId()) {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                    }else {
                        //Check value is null as no data exists before the startTs for series 1
                        Assert.assertNull(value.getValue());
                        //Check time
                    }
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertTrue(bookend);
                }

                @Override
                public void row(IdPointValueTime value) {

                    mutableIndex.increment();
                    count.increment();
                    if(value.getTime() < timestamp.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp.setValue(value.getTime());
                    if(value.getSeriesId() == vo2.getSeriesId()) {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                        seriesId2Counter++;
                    }else {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                        seriesIdCounter++;
                    }
                    if(count.getValue() == 20)
                        throw new QueryCancelledException(new Exception("Exception Test"));
                }

                @Override
                public void lastValue(IdPointValueTime value, boolean bookend) {
                    Assert.fail("Query cancelled, should not get last value");
                }

            });
        }catch(QueryCancelledException e) {
            // noop
        }
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testBookendExceptionInLastValueCallback() {
        MutableInt count = new MutableInt();
        MutableInt lastValueCallCount = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        try {
            this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<>() {

                int seriesIdCounter = 0;
                int seriesId2Counter = 3; //Skip first 3

                @Override
                public void firstValue(IdPointValueTime value, boolean bookend) {

                    mutableIndex.increment();
                    if(value.getTime() < timestamp.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp.setValue(value.getTime());
                    if(value.getSeriesId() == vo2.getSeriesId()) {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                    }else {
                        //Check value is null as no data exists before the startTs for series 1
                        Assert.assertNull(value.getValue());
                        //Check time
                    }
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertTrue(bookend);
                }

                @Override
                public void row(IdPointValueTime value) {

                    mutableIndex.increment();
                    count.increment();
                    if(value.getTime() < timestamp.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp.setValue(value.getTime());
                    if(value.getSeriesId() == vo2.getSeriesId()) {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                        seriesId2Counter++;
                    }else {
                        //Check value
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                        //Check time
                        Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                        seriesIdCounter++;
                    }
                }

                @Override
                public void lastValue(IdPointValueTime value, boolean bookend) {
                    lastValueCallCount.increment();

                    mutableIndex.increment();
                    if(value.getTime() < timestamp.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp.setValue(value.getTime());
                    throw new QueryCancelledException(new Exception("Last Value Callback Exception"));
                }

            });
        }catch(QueryCancelledException e) {
            // noop
        }
        //Since the exception is thrown in last value all the true values should have been sent out already
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
        //Ensure that last value is only called once due to the exception
        Assert.assertEquals(Integer.valueOf(1) , lastValueCallCount.getValue());
    }

    public void testBookendNoDataInBothSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(0);
        this.dao.wideBookendQuery(vos, 0, series2StartTs - 1, false, null, new BookendQueryCallback<>() {
            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                Assert.assertNull(value.getValue());
                Assert.assertEquals(0, value.getTime());
                Assert.assertTrue(bookend);
            }
            @Override
            public void row(IdPointValueTime value) {
                Assert.fail("Should not get any data");
            }
            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                Assert.assertNull(value.getValue());
                Assert.assertEquals(series2StartTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }
        });

        Assert.assertEquals(4, count.intValue());
    }

    public void testBookendEmptySeries() {
        this.dao.wideBookendQuery(List.of(emptyDataPointVO), 0, 10000, false, null, new BookendQueryCallback<>() {
            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {
                Assert.assertTrue(bookend);
                Assert.assertNull(value.getValue());
                Assert.assertEquals(0, value.getTime());
            }
            @Override
            public void row(IdPointValueTime value) {
                Assert.fail("No data should be in series");
            }
            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {
                Assert.assertTrue(bookend);
                Assert.assertNull(value.getValue());
                Assert.assertEquals(10000, value.getTime());
            }
        });
    }

    public void testBookendNoDataInOneSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(0);
        this.dao.wideBookendQuery(vos, 0, startTs, false, null, new BookendQueryCallback<>() {
            int seriesId2Counter = 0;
            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                Assert.assertNull(value.getValue());
                Assert.assertEquals(0, value.getTime());
                Assert.assertTrue(bookend);
            }
            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    Assert.fail("Should not get data for series 1");
                }
            }
            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    //Check value
                    Assert.assertNull(value.getValue());
                    //Check time
                }
                Assert.assertEquals(startTs, value.getTime());
                Assert.assertTrue(bookend);
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(7) , count.getValue());
    }

    public void testBookendMultiplePointValuesNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    //Check value is null as there is no value before startTs
                    Assert.assertNull(value.getValue());
                    //Check time
                }
                Assert.assertEquals(startTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(totalSampleCount + 2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }
                Assert.assertEquals(endTs, value.getTime());
                Assert.assertTrue(bookend);
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
    }

    public void testBookendMultiplePointValuesNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2StartTs - 1);
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, false, null, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                Assert.assertNull(value.getValue());
                Assert.assertEquals(series2StartTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(data.get(value.getSeriesId()).size() - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }
                Assert.assertEquals(series2EndTs + 1, value.getTime());
                Assert.assertTrue(bookend);
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());
    }

    public void testBookendMultiplePointValuesOrderByIdNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(startTs - 1);
        MutableLong timestamp2 = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, true, null, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                }
                Assert.assertEquals(startTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                count.increment();
                if(mutableIndex.getAndIncrement() < data.get(vo1.getSeriesId()).size() + 1) { //1 for end bookend
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(totalSampleCount + 2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }
                Assert.assertEquals(endTs, value.getTime());
                Assert.assertTrue(bookend);
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
    }

    public void testBookendMultiplePointValuesOrderByIdNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2StartTs - 1);
        MutableLong timestamp2 = new MutableLong(series2StartTs - 1);
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, true, null, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value is null as no data exists before the start Ts for series 2
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    //Check time
                }
                Assert.assertNull(value.getValue());
                Assert.assertEquals(series2StartTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                count.increment();
                if(mutableIndex.getAndIncrement() < data.get(vo1.getSeriesId()).size() + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(data.get(value.getSeriesId()).size() - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }
                Assert.assertEquals(series2EndTs + 1, value.getTime());
                Assert.assertTrue(bookend);
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());
    }

    public void testBookendMultiplePointValuesLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, 20, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                }
                Assert.assertEquals(startTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Limited queries bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                }else {
                    //Limited Query Bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    //Limited queries do not have bookends
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testBookendMultiplePointValuesLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2StartTs - 1);
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, false, 20, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                Assert.assertNull(value.getValue());
                Assert.assertEquals(series2StartTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //This has a value before the startTs
                    //Limited query bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                }else {
                    //Ensure bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                    //Limited queries do not have bookends
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testBookendMultiplePointValuesOrderByIdLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(startTs - 1);
        MutableLong timestamp2 = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, true, 20, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                }
                Assert.assertEquals(startTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                if(mutableIndex.getAndIncrement() < 20 + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Limited queries bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }
                Assert.assertEquals(endTs, value.getTime());
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count1.getValue());
        Assert.assertEquals(Integer.valueOf(20), count2.getValue());
    }

    public void testBookendMultiplePointValuesOrderByIdLimitOffsetSeries() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2StartTs - 1);
        MutableLong timestamp2 = new MutableLong(series2StartTs - 1);
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, true, 20, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value is null as no data exists before the start Ts for series 2
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    //Check time
                }
                Assert.assertNull(value.getValue());
                Assert.assertEquals(series2StartTs - 1, value.getTime());
                Assert.assertTrue(bookend);
            }

            @Override
            public void row(IdPointValueTime value) {

                if(mutableIndex.getAndIncrement() < 20 + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Limited but has bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Limited but has bookend
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                }
                Assert.assertEquals(series2EndTs + 1, value.getTime());
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count1.getValue());
        Assert.assertEquals(Integer.valueOf(20), count2.getValue());
    }

    /**
     * Query with only 1 value at the start time for series 2
     */
    public void testSeries1NoDataSeries2OneSampleOrderById() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2StartTs);
        MutableLong timestamp2 = new MutableLong(series2StartTs);

        this.dao.wideBookendQuery(vos, series2StartTs, series2StartTs + 2, true, 20, new BookendQueryCallback<>() {

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value is null as no data exists before the start Ts for series 2
                    Assert.assertEquals(data.get(vo2.getSeriesId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertFalse(bookend);
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertTrue(bookend);
                    count2.increment();
                }
            }

            @Override
            public void row(IdPointValueTime value) {
                Assert.fail("No data in query period, should not call row");
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs + 2, value.getTime());
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs + 2, value.getTime());
                    count2.increment();
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(2), count1.getValue());
        Assert.assertEquals(Integer.valueOf(2), count2.getValue());
    }

    public void testNoStartBookendOrderByIdLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(startTs);
        MutableLong timestamp2 = new MutableLong(startTs);
        this.dao.wideBookendQuery(vos, startTs, endTs, true, 20, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3;

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    Assert.assertEquals(data.get(vo2.getSeriesId()).get(seriesId2Counter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertFalse(bookend);
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertEquals(data.get(vo1.getSeriesId()).get(seriesIdCounter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertFalse(bookend);
                    count2.increment();
                }
            }

            @Override
            public void row(IdPointValueTime value) {

                if(mutableIndex.getAndIncrement() < 20 + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getSeriesId(), value.getSeriesId());
                }else {
                    Assert.assertEquals(vo2.getSeriesId(), value.getSeriesId());
                }
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    count2.increment();
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(21), count1.getValue());
        Assert.assertEquals(Integer.valueOf(21), count2.getValue());
    }

    /**
     * Query with only 1 value at the start time for series 2
     */
    public void testSeries1NoDataSeries2OneSample() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2StartTs);

        this.dao.wideBookendQuery(vos, series2StartTs, series2StartTs + 2, false, 20, new BookendQueryCallback<>() {

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                count.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //Check value is null as no data exists before the start Ts for series 2
                    Assert.assertEquals(data.get(vo2.getSeriesId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertFalse(bookend);

                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertTrue(bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value) {
                Assert.fail("No data in query period, should not call row");
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                count.increment();
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //This has a value before the startTs
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs + 2, value.getTime());
                }else {
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs + 2, value.getTime());
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(4), count.getValue());
    }

    public void testNoStartBookendLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.wideBookendQuery(vos, startTs, endTs, false, 20, new BookendQueryCallback<>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3;

            @Override
            public void firstValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    Assert.assertEquals(data.get(vo2.getSeriesId()).get(seriesId2Counter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertFalse(bookend);
                    count1.increment();
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertEquals(data.get(vo1.getSeriesId()).get(seriesIdCounter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertFalse(bookend);
                    count2.increment();
                }
            }

            @Override
            public void row(IdPointValueTime value) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getSeriesId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, boolean bookend) {

                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getSeriesId() == vo2.getSeriesId()) {
                    //This has a value before the startTs
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    count1.increment();
                }else {
                    Assert.assertTrue(bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getSeriesId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    count2.increment();
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(11), count1.getValue());
        Assert.assertEquals(Integer.valueOf(11), count2.getValue());
    }
}
