/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.m2m2.rt.dataImage.DataPointEventMulticaster;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;

/**
 *
 * @author Terry Packer
 */
public class DataPointEventMulticasterTest {

    @Test
    public void testRemoveListener() {
        //This test replicates the potential for the re-run of the remapping function of the DataPointListener's Multicaster during a ConcurrentHashMap.compute method.
        TestDataPointListener l = new TestDataPointListener("Listener One", 1);
        TestDataPointListener l2 = new TestDataPointListener("Listener Two", 2);
        
        DataPointListener listener = DataPointEventMulticaster.add(l, l2);
        for(int i=0; i<10; i++)
            DataPointEventMulticaster.remove(l2, l);
        
        DataPointListener[] listeners = DataPointEventMulticaster.getListeners(listener);
        assertEquals(2, listeners.length);
        assertEquals("Listener One", listeners[0].getListenerName());
        assertEquals("Listener Two", listeners[1].getListenerName());
        
    }
    
}
