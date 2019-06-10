/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

/**
 * Test the accuracy of the DataPointRT point value query methods
 * 
 * @author Terry Packer
 *
 */
public class DataPointRTQueriesTest extends AbstractPointValueCacheTestBase {

    @Test
    public void getPointValueBefore() {

        setupRuntime(10);
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        //Query for them
        PointValueTime before = rt.getPointValueBefore(timer.currentTimeMillis());
        
        assertNotNull(before);
        assertEquals(values.get(values.size() - 1), before);
        
        //Insert a bunch to ensure we are querying from the cache
        values = insertValuesIntoRuntime(5000);
        before = rt.getPointValueBefore(values.get(4500).getTime());
        
        assertNotNull(before);
        assertEquals(values.get(4499), before);
        
        //Test the null before
        assertNull(rt.getPointValueBefore(0));
        
    }
    
    @Test
    public void getPointValueAt() {
        setupRuntime(10);
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        //Query for them
        PointValueTime at = rt.getPointValueAt(3000);
        
        assertNotNull(at);
        assertEquals(values.get(3), at);
        
        //Insert a bunch to ensure we are querying from the cache
        values = insertValuesIntoRuntime(5000);
        at = rt.getPointValueAt(values.get(2500).getTime());
        
        assertNotNull(at);
        assertEquals(values.get(2500), at);
        
        //Test a null value 
        assertNull(rt.getPointValueAt(500));
    }
    
    @Test
    public void getPointValueAfter() {
        setupRuntime(10);
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        //Query for them
        PointValueTime after = rt.getPointValueAfter(3000);
        
        assertNotNull(after);
        assertEquals(values.get(3), after);
        
        //Insert a bunch to ensure we are querying from the cache
        values = insertValuesIntoRuntime(5000);
        after = rt.getPointValueAfter(values.get(2500).getTime());
        
        assertNotNull(after);
        assertEquals(values.get(2500), after);
        
        //Test null value after now
        assertNull(rt.getPointValueAfter(timer.currentTimeMillis()));
    }
    
    @Test
    public void getLatestPointValues() {
        setupRuntime(10);
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        //Query for them
        List<PointValueTime> latest = rt.getLatestPointValues(3);
        
        assertEquals(3, latest.size());
        //Check the order (cache is time descending)
        assertEquals(values.get(4), latest.get(0));
        assertEquals(values.get(3), latest.get(1));
        assertEquals(values.get(2), latest.get(2));
        
        //Insert a bunch to ensure we are querying from the cache
        values.addAll(insertValuesIntoRuntime(5000));
        latest = rt.getLatestPointValues(1000);
        
        assertEquals(1000, latest.size());
        //Check the order (latest is time descending)
        for(int i=0; i<latest.size(); i++) {
            assertEquals(values.get(values.size() - (i + 1)), latest.get(i));
        }
    }
    
    @Test
    public void getPointValuesSince() {
        setupRuntime(10);
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        //Query for them
        List<PointValueTime> since = rt.getPointValues(2000);
        
        assertEquals(3, since.size());
        for(int i=0; i<3; i++)
            assertEquals(values.get(i + 2), since.get(i));
        
        //Insert a bunch to ensure we are querying from the cache
        values.addAll(insertValuesIntoRuntime(5000));
        since = rt.getPointValues(values.get(3500).getTime());
        
        assertEquals(1505, since.size());
        for(int i=0; i<1505; i++) {
            assertEquals(values.get(i + 3500), since.get(i));
        }
        
        //Test no values after now
        assertEquals(0, rt.getPointValues(timer.currentTimeMillis()).size());
    }
    
    @Test
    public void getPointValuesBetween() {
        setupRuntime(10);
        
        //Insert some test data
        List<PointValueTime> values = insertValuesIntoRuntime(5);

        //Query for them
        List<PointValueTime> between = rt.getPointValuesBetween(2000, 4000);
        
        assertEquals(2, between.size());
        for(int i=0; i<2; i++)
            assertEquals(values.get(i + 2), between.get(i));
        
        //Insert a bunch to ensure we are querying from the cache
        values.addAll(insertValuesIntoRuntime(5000));
        between = rt.getPointValuesBetween(values.get(3500).getTime(), values.get(4500).getTime());
        
        assertEquals(1000, between.size());
        for(int i=0; i<1000; i++) {
            assertEquals(values.get(i + 3500), between.get(i));
        }
        
        //Test no values between some future date
        assertEquals(0, rt.getPointValuesBetween(timer.currentTimeMillis(), timer.currentTimeMillis() + 1000000).size());
    }
    
}
