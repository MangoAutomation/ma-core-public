/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

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
    
    protected Integer seriesId = 1;
    protected Integer seriesId2 = 2;
    protected List<Integer> ids;
    protected Map<Integer, List<PointValueTime>> data;
    protected static long startTs;
    protected static long endTs;
    protected int totalSampleCount;
    protected final PointValueDao dao;
    
    public NumericPointValueDaoTestHelper(PointValueDao dao) {
        this.dao = dao;
        
        this.ids = new ArrayList<>();
        this.ids.add(seriesId);
        this.ids.add(seriesId2);
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
        
        for(Integer id : ids)
            this.data.put(id, new ArrayList<>());
        
        //Insert a few samples for series 2 before our time
        long time = startTs - (1000 * 60 * 15);
        PointValueTime p2vt = new PointValueTime(-3.0, time);
        this.dao.savePointValueSync(seriesId2, p2vt, null);
        this.data.get(seriesId2).add(p2vt);
        
        time = startTs - (1000 * 60 * 10);
        p2vt = new PointValueTime(-2.0, time);
        this.dao.savePointValueSync(seriesId2, p2vt, null);
        this.data.get(seriesId2).add(p2vt);
        
        time = startTs - (1000 * 60 * 5);
        p2vt = new PointValueTime(-1.0, time);
        this.dao.savePointValueSync(seriesId2, p2vt, null);
        this.data.get(seriesId2).add(p2vt);
        
        time = startTs;
        //Insert a sample every 5 minutes
        double value = 0.0;
        while(time < endTs){
            PointValueTime pvt = new PointValueTime(value, time);
            this.data.get(seriesId).add(pvt);
            this.data.get(seriesId2).add(pvt);
            this.dao.savePointValueSync(seriesId, pvt, null);
            this.dao.savePointValueSync(seriesId2, pvt, null);
            time = time + 1000 * 60 * 5;
            totalSampleCount++;
            value++;
        }
        
        //Add a few more samples for series 2 after our time
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(seriesId2, p2vt, null);
        this.data.get(seriesId2).add(p2vt);
        
        time = time + (1000 * 60 * 5);
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(seriesId2, p2vt, null);
        this.data.get(seriesId2).add(p2vt);
        
        time = time + (1000 * 60 * 5);
        p2vt = new PointValueTime(value++, time);
        this.dao.savePointValueSync(seriesId2, p2vt, null);
        this.data.get(seriesId2).add(p2vt);
    }

    /**
     * Call after every test
     */
    public void after() {
        this.dao.deleteAllPointData();
    }
    
    /* Latest Multiple w/ callback Test Methods */
    
    public void testLatestMultiplePointValuesNoLimit() {
        MutableInt count = new MutableInt();
        this.dao.getLatestPointValues(ids, endTs, false, null, new PVTQueryCallback<IdPointValueTime>() {
            
            int seriesIdCounter = data.get(seriesId).size() - 1;
            int seriesId2Counter = data.get(seriesId2).size() - 4; //Start before last 3 samples (extra)
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(totalSampleCount * 2 + 3) , count.getValue());
    }
    
    public void testLatestMultiplePointValuesOrderByIdNoLimit() {
        MutableInt count = new MutableInt();
        this.dao.getLatestPointValues(ids, endTs, true, null, new PVTQueryCallback<IdPointValueTime>() {
            
            int seriesIdCounter = data.get(seriesId).size() - 1;
            int seriesId2Counter = data.get(seriesId2).size() - 4; //Start before last 3 samples (extra)
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(index < data.get(seriesId).size()) {
                    //Should be first id
                    Assert.assertEquals((int)seriesId, value.getId());
                }else {
                    Assert.assertEquals((int)seriesId2, value.getId());
                }
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(totalSampleCount * 2 + 3) , count.getValue());

    }
    
    public void testLatestMultiplePointValuesLimit() {
        MutableInt count = new MutableInt();
        this.dao.getPointValuesBetween(ids, startTs, endTs, false, 20, new PVTQueryCallback<IdPointValueTime>() {
            
            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(20), count.getValue());
    }
    
    public void testLatestMultiplePointValuesOrderByIdLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        this.dao.getPointValuesBetween(ids, startTs, endTs, true, 20, new PVTQueryCallback<IdPointValueTime>() {
            
            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                if(value.getId() == seriesId2) {
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
            
        });
        Assert.assertEquals(new Integer(20), count1.getValue());
        Assert.assertEquals(new Integer(20), count2.getValue());
    }

    /* Values Between Tests */
    public void testRangeMultiplePointValuesNoLimit() {
        MutableInt count = new MutableInt();
        this.dao.getPointValuesBetween(ids, startTs, endTs, false, null, new PVTQueryCallback<IdPointValueTime>() {
            
            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(totalSampleCount * 2) , count.getValue());
    }
    
    public void testRangeMultiplePointValuesOrderByIdNoLimit() {
        MutableInt count = new MutableInt();
        this.dao.getPointValuesBetween(ids, startTs, endTs, true, null, new PVTQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(index < data.get(seriesId).size()) {
                    //Should be first id
                    Assert.assertEquals((int)seriesId, value.getId());
                }else {
                    Assert.assertEquals((int)seriesId2, value.getId());
                }
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(totalSampleCount * 2) , count.getValue());
    }
    
    public void testRangeMultiplePointValuesLimit() {
        MutableInt count = new MutableInt();
        this.dao.getPointValuesBetween(ids, startTs, endTs, false, 20, new PVTQueryCallback<IdPointValueTime>() {
            
            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(20) , count.getValue());
    }
    
    public void testRangeMultiplePointValuesOrderByIdLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        
        this.dao.getPointValuesBetween(ids, startTs, endTs, true, 20, new PVTQueryCallback<IdPointValueTime>() {
            
            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                if(index < 20) {
                    //Should be first id
                    Assert.assertEquals((int)seriesId, value.getId());
                }else {
                    Assert.assertEquals((int)seriesId2, value.getId());
                }
                if(value.getId() == seriesId2) {
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
            
        });
        Assert.assertEquals(new Integer(20), count1.getValue());
        Assert.assertEquals(new Integer(20), count2.getValue());
    }
 
    /* Bookend Tests */
    public void testBookendMultiplePointValuesNoLimit() {
        MutableInt count = new MutableInt();
        this.dao.wideBookendQuery(ids, startTs - 1, endTs, false, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                if(value.getId() == seriesId2) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(value.getId() == seriesId2) {
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
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(totalSampleCount * 2) , count.getValue());   
    }
    
    public void testBookendMultiplePointValuesOrderByIdNoLimit() {
        MutableInt count = new MutableInt();
        this.dao.wideBookendQuery(ids, startTs - 1, endTs, true, null, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                if(value.getId() == seriesId2) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(index < data.get(seriesId).size()) {
                    //Should be first id
                    Assert.assertEquals((int)seriesId, value.getId());
                }else {
                    Assert.assertEquals((int)seriesId2, value.getId());
                }
                if(value.getId() == seriesId2) {
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
                if(value.getId() == seriesId2) {
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
        Assert.assertEquals(new Integer(totalSampleCount * 2) , count.getValue()); 
    }
    
    public void testBookendMultiplePointValuesLimit() {
        MutableInt count = new MutableInt();
        this.dao.wideBookendQuery(ids, startTs - 1, endTs, false, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                if(value.getId() == seriesId2) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                count.increment();
                if(value.getId() == seriesId2) {
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
                if(value.getId() == seriesId2) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            
        });
        Assert.assertEquals(new Integer(20) , count.getValue()); 
    }
    
    public void testBookendMultiplePointValuesOrderByIdLimit() {
        MutableInt count1 = new MutableInt();
        MutableInt count2 = new MutableInt();
        this.dao.wideBookendQuery(ids, startTs - 1, endTs, true, 20, new BookendQueryCallback<IdPointValueTime>() {

            int seriesIdCounter = 0;
            int seriesId2Counter = 3; //Skip first 3
            
            @Override
            public void firstValue(IdPointValueTime value, int index, boolean bookend)
                    throws IOException {
                if(value.getId() == seriesId2) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(2).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(0).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(startTs - 1, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            
            @Override
            public void row(IdPointValueTime value, int index) throws IOException {
                if(index < 20) {
                    //Should be first id
                    Assert.assertEquals((int)seriesId, value.getId());
                }else {
                    Assert.assertEquals((int)seriesId2, value.getId());
                }
                if(value.getId() == seriesId2) {
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
                if(value.getId() == seriesId2) {
                    //This has a value before the startTs
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesId2Counter - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }else {
                    //Check value
                    Assert.assertEquals(data.get(value.getId()).get(seriesIdCounter - 1).getDoubleValue(), value.getDoubleValue(), 0.001);
                    //Check time
                    Assert.assertEquals(endTs, value.getTime());
                    Assert.assertEquals(true, bookend);
                }
            }
            
        });
        Assert.assertEquals(new Integer(20), count1.getValue());
        Assert.assertEquals(new Integer(20), count2.getValue());
    }
}
