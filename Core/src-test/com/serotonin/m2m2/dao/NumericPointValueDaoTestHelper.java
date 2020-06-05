/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Assert;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 *
 * Class to keep all the test logic in one place so we
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
     * Insert some test data.
     * Call before every test.
     */
    public void before() {
        //Start back 30 days
        endTs = System.currentTimeMillis();
        startTs = endTs - (30l * 24l * 60l * 60l * 1000l);

        for(DataPointVO vo : vos)
            this.data.put(vo.getId(), new ArrayList<>());

        //Insert a few samples for series 2 before our time
        series2StartTs = startTs - (1000 * 60 * 15);
        long time = series2StartTs;
        PointValueTime p2vt = new PointValueTime(-3.0, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getId()).add(p2vt);

        time = startTs - (1000 * 60 * 10);
        p2vt = new PointValueTime(-2.0, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getId()).add(p2vt);

        time = startTs - (1000 * 60 * 5);
        p2vt = new PointValueTime(-1.0, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getId()).add(p2vt);

        time = startTs;
        //Insert a sample every 5 minutes
        double value = 0.0;
        while(time < endTs){
            PointValueTime pvt = new PointValueTime(value, time);
            this.data.get(vo1.getId()).add(pvt);
            this.data.get(vo2.getId()).add(pvt);
            this.dao.savePointValueSync(vo1, pvt, null);
            this.dao.savePointValueSync(vo2, pvt, null);
            time = time + 1000 * 60 * 5;
            totalSampleCount++;
            value++;
        }

        //Add a few more samples for series 2 after our time
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getId()).add(p2vt);

        time = time + (1000 * 60 * 5);
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getId()).add(p2vt);

        time = time + (1000 * 60 * 5);
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(vo2, p2vt, null);
        this.data.get(vo2.getId()).add(p2vt);
        this.series2EndTs = time;
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
        this.dao.getLatestPointValues(vos, endTs, false, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 4; //Start before last 3 samples (extra)
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter--;
                }
                if(count.getValue() == 20)
                    throw new IOException("Exception Test");
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testLatestNoDataInBothSeries() {
        MutableInt count = new MutableInt();
        this.dao.getLatestPointValues(vos, series2StartTs, false, null, new PVTQueryCallback<IdPointValueTime>() {
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
            }

        });
        Assert.assertEquals(Integer.valueOf(0), count.getValue());
    }

    public void testLatestNoDataInOneSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.getLatestPointValues(vos, startTs, false, null, new PVTQueryCallback<IdPointValueTime>() {
            int seriesId2Counter = 2;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
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
        this.dao.getLatestPointValues(vos, endTs, false, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 4; //Start before last 3 samples (extra)
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getLatestPointValues(vos, series2EndTs + 1, false, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 1;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getLatestPointValues(vos, endTs, true, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 4; //Start before last 3 samples (extra)
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(index < data.get(vo1.getId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getLatestPointValues(vos, series2EndTs + 1, true, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 1;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(index < data.get(vo1.getId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getLatestPointValues(vos, endTs, false, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 4;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getLatestPointValues(vos, series2EndTs + 1, false, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 1;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() > timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getLatestPointValues(vos, endTs, true, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 4;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(index < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getLatestPointValues(vos, series2EndTs + 1, true, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = data.get(vo1.getId()).size() - 1;
            int seriesId2Counter = data.get(vo2.getId()).size() - 1;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(index < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() > timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter--;
                }else {
                    if(value.getTime() > timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, startTs, endTs, false, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
                if(count.getValue() == 20)
                    throw new IOException("Exception Test");
            }

        });
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testBetweenNoDataInBothSeries() {
        MutableInt count = new MutableInt();
        this.dao.getPointValuesBetween(vos, 0, series2StartTs - 1, false, null, new PVTQueryCallback<IdPointValueTime>() {
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
            }

        });
        Assert.assertEquals(Integer.valueOf(0), count.getValue());
    }

    public void testBetweenNoDataInOneSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(0);
        this.dao.getPointValuesBetween(vos, 0, startTs, false, null, new PVTQueryCallback<IdPointValueTime>() {
            int seriesId2Counter = 0;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, startTs, endTs, false, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs + 1, false, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, startTs, endTs, true, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(index < data.get(vo1.getId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs+1, true, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(index < data.get(vo1.getId()).size()) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, startTs, endTs, false, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs, false, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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

        this.dao.getPointValuesBetween(vos, startTs, endTs, true, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(index < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
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
        this.dao.getPointValuesBetween(vos, series2StartTs, series2EndTs, true, 20, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(index < 20) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(20), count1.getValue());
        Assert.assertEquals(Integer.valueOf(20), count2.getValue());
    }

    public void testWideQueryNoData() {
        this.dao.wideQuery(emptyDataPointVO, 0, 100, new WideQueryCallback<PointValueTime>() {

            @Override
            public void preQuery(PointValueTime value) {
                Assert.fail("Should not have data");
            }

            @Override
            public void row(PointValueTime value, int index) {
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
        this.dao.wideQuery(vo1, startTs - 1, endTs - 1, new WideQueryCallback<PointValueTime>() {
            int counter = 0;
            @Override
            public void preQuery(PointValueTime value) {
                Assert.fail("Should not have data");
            }

            @Override
            public void row(PointValueTime value, int index) {
                Assert.assertEquals(data.get(vo1.getId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void postQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }
        });
    }

    public void testWideQuery() {
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.wideQuery(vo1, startTs + 1, endTs - 1, new WideQueryCallback<PointValueTime>() {
            int counter = 0;
            @Override
            public void preQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void row(PointValueTime value, int index) {
                Assert.assertEquals(data.get(vo1.getId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void postQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }
        });
    }

    public void testWideQueryNoAfter() {
        MutableLong timestamp = new MutableLong(startTs);
        this.dao.wideQuery(vo1, startTs + 1, endTs, new WideQueryCallback<PointValueTime>() {
            int counter = 0;
            @Override
            public void preQuery(PointValueTime value) {
                Assert.assertEquals(data.get(vo1.getId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                counter++;
            }

            @Override
            public void row(PointValueTime value, int index) {
                Assert.assertEquals(data.get(vo1.getId()).get(counter).getDoubleValue(), value.getDoubleValue(), 0.001);
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
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<IdPointValueTime>() {
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                throw new IOException("First Value Callback Exception");
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.fail("Query cancelled, should not get any rows");
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.fail("Query cancelled, should not get last value");
            }

        });
    }

    public void testBookendExceptionInRowCallback() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
                if(count.getValue() == 20)
                    throw new IOException("Exception Test");
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.fail("Query cancelled, should not get last value");
            }

        });
        Assert.assertEquals(Integer.valueOf(20) , count.getValue());
    }

    public void testBookendExceptionInLastValueCallback() {
        MutableInt count = new MutableInt();
        MutableInt lastValueCallCount = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                lastValueCallCount.increment();
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                throw new IOException("Last Value Callback Exception");
            }

        });
        //Since the exception is thrown in last value all the true values should have been sent out already
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
        //Ensure that last value is only called once due to the exception
        Assert.assertEquals(Integer.valueOf(1) , lastValueCallCount.getValue());
    }

    public void testBookendNoDataInBothSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(0);
        this.dao.wideBookendQuery(vos, 0, series2StartTs - 1, false, null, new BookendQueryCallback<IdPointValueTime>() {
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value is null as no data exists before time 0
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(0, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before time 0
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(0, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.fail("Should not get any data");
            }
            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value is null as no data exists before time 0
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before time 0
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
        });

        Assert.assertEquals(4, count.intValue());
    }

    public void testBookendEmptySeries() {
        this.dao.wideBookendQuery(Arrays.asList(emptyDataPointVO), 0, 10000, false, null, new BookendQueryCallback<IdPointValueTime>() {
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertTrue(bookend);
                Assert.assertNull(value.getValue());
                Assert.assertEquals(0, value.getTime());
            }
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.fail("No data should be in series");
            }
            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
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
        this.dao.wideBookendQuery(vos, 0, startTs, false, null, new BookendQueryCallback<IdPointValueTime>() {
            int seriesId2Counter = 0;
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value is null as no data exists before time 0
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(0, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before time 0
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(0, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    Assert.fail("Should not get data for series 1");
                }
            }
            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

        });
        //Total is all samples + the extra 3 at the beginning of series2
        Assert.assertEquals(Integer.valueOf(7) , count.getValue());
    }

    public void testBookendMultiplePointValuesNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as there is no value before startTs
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(totalSampleCount + 2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
    }

    public void testBookendMultiplePointValuesNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(series2StartTs - 1);
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, false, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value is null as no data exists before the start time for series 2
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(data.get(value.getId()).size() - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());
    }

    public void testBookendMultiplePointValuesOrderByIdNoLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(startTs - 1);
        MutableLong timestamp2 = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, true, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(index < data.get(vo1.getId()).size() + 1) { //1 for end bookend
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(totalSampleCount + 2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2) , count.getValue());
    }

    public void testBookendMultiplePointValuesOrderByIdNoLimitOffsetSeries() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp1 = new MutableLong(series2StartTs - 1);
        MutableLong timestamp2 = new MutableLong(series2StartTs - 1);
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, true, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value is null as no data exists before the start Ts for series 2
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(index < data.get(vo1.getId()).size() + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(data.get(value.getId()).size() - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(totalSampleCount - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

        });
        Assert.assertEquals(Integer.valueOf(totalSampleCount * 2 + 6) , count.getValue());
    }

    public void testBookendMultiplePointValuesLimit() {
        MutableInt count = new MutableInt();
        MutableInt mutableIndex = new MutableInt();
        MutableLong timestamp = new MutableLong(startTs - 1);
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, false, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Limited queries bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                }else {
                    //Limited Query Bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
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
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, false, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value is null as no data exists before the start Ts for series 2
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                count.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //This has a value before the startTs
                    //Limited query bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                }else {
                    //Ensure bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
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
        this.dao.wideBookendQuery(vos, startTs - 1, endTs, true, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(index < 20 + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Check bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Limited queries bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                }
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
        this.dao.wideBookendQuery(vos, series2StartTs - 1, series2EndTs + 1, true, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 0;

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value is null as no data exists before the start Ts for series 2
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(index < 20 + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    //Limited but has bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Limited but has bookend
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2EndTs + 1, value.getTime());
                }
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

        this.dao.wideBookendQuery(vos, series2StartTs, series2StartTs + 2, true, 20, new BookendQueryCallback<IdPointValueTime>() {

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //Check value is null as no data exists before the start Ts for series 2
                    Assert.assertEquals(data.get(vo2.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertEquals(false, bookend);
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                    count2.increment();
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.fail("No data in query period, should not call row");
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs + 2, value.getTime());
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    Assert.assertEquals(true, bookend);
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
        this.dao.wideBookendQuery(vos, startTs, endTs, true, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3;

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    Assert.assertEquals(data.get(vo2.getId()).get(seriesId2Counter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertEquals(false, bookend);
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertEquals(data.get(vo1.getId()).get(seriesIdCounter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertEquals(false, bookend);
                    count2.increment();
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(index < 20 + 1) {
                    //Should be first id
                    Assert.assertEquals(vo1.getId(), value.getId());
                }else {
                    Assert.assertEquals(vo2.getId(), value.getId());
                }
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getId() == vo2.getId()) {
                    if(value.getTime() < timestamp2.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp2.setValue(value.getTime());
                    //This has a value before the startTs
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    count1.increment();
                }else {
                    if(value.getTime() < timestamp1.getValue())
                        Assert.fail("Timestamp out of order.");
                    timestamp1.setValue(value.getTime());
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
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

        this.dao.wideBookendQuery(vos, series2StartTs, series2StartTs + 2, false, 20, new BookendQueryCallback<IdPointValueTime>() {

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                count.increment();
                if(value.getId() == vo2.getId()) {
                    //Check value is null as no data exists before the start Ts for series 2
                    Assert.assertEquals(data.get(vo2.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertEquals(false, bookend);

                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertNull(value.getValue());
                    //Check time
                    Assert.assertEquals(series2StartTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.fail("No data in query period, should not call row");
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                count.increment();
                if(value.getId() == vo2.getId()) {
                    //This has a value before the startTs
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(series2StartTs + 2, value.getTime());
                }else {
                    Assert.assertEquals(true, bookend);
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
        this.dao.wideBookendQuery(vos, startTs, endTs, false, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3;

            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    Assert.assertEquals(data.get(vo2.getId()).get(seriesId2Counter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertEquals(false, bookend);
                    count1.increment();
                }else {
                    //Check value is null as no data exists before the startTs for series 1
                    Assert.assertEquals(data.get(vo1.getId()).get(seriesIdCounter++).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs, value.getTime());
                    Assert.assertEquals(false, bookend);
                    count2.increment();
                }
            }

            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    count2.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter).getTime(), value.getTime());
                    seriesId2Counter++;
                }else {
                    count1.increment();
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter).getTime(), value.getTime());
                    seriesIdCounter++;
                }
            }

            @Override
            public void lastValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                Assert.assertEquals(mutableIndex.intValue(), index);
                mutableIndex.increment();
                if(value.getTime() < timestamp.getValue())
                    Assert.fail("Timestamp out of order.");
                timestamp.setValue(value.getTime());
                if(value.getId() == vo2.getId()) {
                    //This has a value before the startTs
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesId2Counter).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    count1.increment();
                }else {
                    Assert.assertEquals(true, bookend);
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(--seriesIdCounter).getDoubleValue(), value.getDoubleValue(), 0.001);
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
