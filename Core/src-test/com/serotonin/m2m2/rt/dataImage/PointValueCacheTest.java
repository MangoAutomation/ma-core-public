/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

/**
 * Tests for cache expansion, contraction and unsaved values tracking
 * 
 * @author Terry Packer
 *
 */
public class PointValueCacheTest extends AbstractPointValueCacheTestBase {
    
    @Test
    public void testBackDates() {
        setupRuntime(10);
        
        //Fast forward to 10s
        currentValue = 5;
        timer.fastForwardTo(timer.currentTimeMillis() + 10000);
        
        List<PointValueTime> values = insertValuesIntoRuntime(10);
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        //Check the order (cache is time descending)
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Insert values in the past
        PointValueTime value = new PointValueTime(0.0d, 0);
        rt.updatePointValue(value);
        values.add(0, value);
        currentValue++;
        
        value = new PointValueTime(1.0d, 1000);
        rt.updatePointValue(value);
        values.add(0, value);
        currentValue++;
        
        value = new PointValueTime(2.0d, 2000);
        rt.updatePointValue(value);
        values.add(0, value);
        currentValue++;
        
        value = new PointValueTime(3.0d, 3000);
        rt.updatePointValue(value);
        values.add(0, value);
        currentValue++;
        
        value = new PointValueTime(4.0d, 4000);
        rt.updatePointValue(value);
        values.add(0, value);
        currentValue++;
        
        //Validate that the cache does not contain these values, the cache should be size 10
        latest = rt.getLatestPointValues(10);
        List<PointValueTime> cache = rt.getCacheCopy();
        assertEquals(10, latest.size());
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i + 5), cache.get(cache.size() - (i + 1)));
            assertEquals(values.get(i + 5), latest.get(latest.size() - (i + 1)));
        }
        
        //Reset the cache and see that is has the latest 10 values
        rt.resetValues();
        latest = rt.getLatestPointValues(10);
        cache = rt.getCacheCopy();
        assertEquals(10, latest.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i + 5), cache.get(cache.size() - (i + 1)));
            assertEquals(values.get(i + 5), latest.get(latest.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }

    @Test
    public void testFutureDates() {
        setupRuntime(10);
        
        List<PointValueTime> values = insertValuesIntoRuntime(10);
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        //Check the order (cache is time descending)
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Insert values in the future
        PointValueTime value = new PointValueTime(currentValue, timer.currentTimeMillis());
        rt.updatePointValue(value);
        values.add(value);
        currentValue++;
        
        value = new PointValueTime(currentValue, timer.currentTimeMillis() + 1000);
        rt.updatePointValue(value);
        values.add(value);
        currentValue++;
        
        value = new PointValueTime(currentValue, timer.currentTimeMillis() + 2000);
        rt.updatePointValue(value);
        values.add(value);
        currentValue++;
        
        value = new PointValueTime(currentValue, timer.currentTimeMillis() + 3000);
        rt.updatePointValue(value);
        values.add(value);
        currentValue++;
        
        value = new PointValueTime(currentValue, timer.currentTimeMillis() + 4000);
        rt.updatePointValue(value);
        values.add(value);
        currentValue++;
        
        //Validate that the cache contains these values, note the cache copy may be larger than 10 here
        latest = rt.getLatestPointValues(10);
        List<PointValueTime> cache = rt.getCacheCopy();
        int cacheOffset = cache.size() - latest.size();
        assertEquals(10, latest.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i + 5), cache.get(cache.size() - (i + 1 + cacheOffset)));
            assertEquals(values.get(i + 5), latest.get(latest.size() - (i + 1)));
        }
        
        //Reset the cache and see that is has the latest 10 values
        rt.resetValues();
        latest = rt.getLatestPointValues(10);
        cache = rt.getCacheCopy();
        assertEquals(10, latest.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i + 5), cache.get(cache.size() - (i + 1)));
            assertEquals(values.get(i + 5), latest.get(latest.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
        
    }
    
    @Test 
    public void testResetBefore() {
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoDatabase(5);

        setupRuntime(10);
        
        //There should only be 5 values
        List<PointValueTime> cache = rt.getCacheCopy();
        assertEquals(5, cache.size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<5; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
        }
        
        //Insert another 10 values
        values.addAll(insertValuesIntoRuntime(5));
        
        //Reset cache, this will expand it to max
        rt.resetValues((1000 * 3) + 1);
        ensureValuesInDatabase(10, 5);
        
        //Confirm cache is expanded and correct
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
        }
        
        //Get all cached values
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
        
        //Totally reset the cache
        rt.resetValues((values.size() * 1000) + 1);
        //Confirm cache is expanded and correct
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
        }
        
        //Reset the cache in a way that no values are removed
        rt.resetValues(0);
        //Confirm cache is expanded and correct
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }
    
    @Test
    public void testLatestPointValuesSynchronization() {
        setupRuntime(1);
        List<PointValueTime> values = insertValuesIntoRuntime(10);
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        //Check the order (cache is time descending)
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        ensureValuesInDatabase(10, 5);
        rt.resetValues();
        
        //Try expanding the cache
        latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }
    
    @Test
    public void testCacheSize1() {
        setupRuntime(1);
        
        List<PointValueTime> values = insertValuesIntoRuntime(10);
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        //Check the order (cache is time descending)
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        ensureValuesInDatabase(10, 5);
        rt.resetValues();
        
        latest = rt.getLatestPointValues(1);
        assertEquals(1, latest.size());
        assertEquals(values.get(9), latest.get(0));
        
        //Try expanding the cache
        latest = rt.getLatestPointValues(10);
        assertEquals(10, latest.size());
        
        //Check the order (cache is time descending)
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }
    
    @Test
    public void testManyUnsavedInCache() throws InterruptedException {
        setupRuntime(10);
        
        //Insert some test data
        int inserted = 1000;
        List<PointValueTime> values = insertValuesIntoRuntime(inserted);
        
        //Get a snapshot of the cache in an expanded state
        List<PointValueTime> cacheCopy = rt.getCacheCopy();
        //Ensure the order is correct
        int valuePos = values.size() - 1;
        for(int i=0; i<cacheCopy.size(); i++) {
            assertEquals(values.get(valuePos), cacheCopy.get(i));
            valuePos--;
        }
        
        List<PointValueTime> latest = rt.getLatestPointValues(inserted);
        assertEquals(inserted, latest.size());
        //Check the order (cache is time descending)
        for(int i=0; i<inserted; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        ensureValuesInDatabase(inserted, 50);
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }
    
    /**
     * Insert 5 values - confirm they are in the cache
     * 
     * Insert 5 more values - confirm they are in the cache
     * 
     * Insert 5 more values - reset cache until until it drops to size 10
     * 
     * Confirm the 10 values in cache are the latest 10
     * 
     * Query for all 15 values inserted, this expands the cache
     * @throws InterruptedException
     */
    @Test
    public void testSaveCallbackPrune() throws InterruptedException {
        setupRuntime(10);
        
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
        
        //This cache will very likely be > 10 as the point values are in the batch writer
        List<PointValueTime> cacheCopy = rt.getCacheCopy();
        
        rt.resetValues();
        ensureValuesInDatabase(15, 5);
        
        //assert cache contents, there will have been 15 inserted and we will compare the latest 10 which should be
        // in the cache
        cacheCopy = rt.getCacheCopy();
        assertEquals(10, cacheCopy.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i + 5), cacheCopy.get(cacheCopy.size() - (i + 1)));
        }
        
        //Check for all 15 values
        latest = rt.getLatestPointValues(15);
        assertEquals(15, latest.size());
        for(int i=0; i<15; i++) {
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }
    
    @Test
    public void testCacheReset() {
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoDatabase(5);

        setupRuntime(10);
        
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
        ensureValuesInDatabase(10, 5);
        
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }
    
    
    @Test
    public void testCacheGetLatestPointValues() {
        
        setupRuntime(10);
        
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
        
        //Expand cache
        List<PointValueTime> latest = rt.getLatestPointValues(10);
        cache = rt.getCacheCopy();
        assertEquals(10, cache.size());
        for(int i=0; i<10; i++) {
            assertEquals(values.get(i), cache.get(cache.size() - (i + 1)));
            assertEquals(values.get(i), latest.get(latest.size() - (i + 1)));
        }
        
        //Test getLatestPointValue via the reset
        assertEquals(values.get(values.size() - 1), rt.getPointValue());
    }
}
