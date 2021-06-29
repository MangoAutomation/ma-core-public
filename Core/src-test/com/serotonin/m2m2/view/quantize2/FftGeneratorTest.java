/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.quantize2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * @author Terry Packer
 *
 */
public class FftGeneratorTest {
    
    @Test
    public void testSimplePollPeriodDetection() {
        
        int length = 100;
        //Generate samples
        long time = 0l;
        FftGenerator gen = new FftGenerator(length);
        for(int i=0; i<length - 1; i++) {
            gen.data(new PointValueTime(1.0, time));
            time += 1000l;
        }
        time += 1000l;
        gen.done(new PointValueTime(1.0, time));
        
        assertEquals(1000.0d, gen.getAverageSamplePeriodMs(), 0.00000001);
    }

    
    @Test
    public void testAccuratePollPeriodDetection() {
        
        int length = 100;
        long pollPeriodMs = 1333l;
        //Generate samples
        long time = 0l;
        FftGenerator gen = new FftGenerator(length);
        for(int i=0; i<length - 1; i++) {
            gen.data(new PointValueTime(1.0, time));
            time += pollPeriodMs;
        }
        time += pollPeriodMs;
        gen.done(new PointValueTime(1.0, time));
        
        assertEquals((double)pollPeriodMs, gen.getAverageSamplePeriodMs(), 0.00000001);
    }
}
