/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.statistics;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * 
 *
 * @author Terry Packer
 */
public class NoStatisticsGenerator implements StatisticsGenerator {

    private boolean done = false;
    
    private long periodStart;
    private long periodEnd;
    private List<IValueTime<DataValue>> values;
    
    public NoStatisticsGenerator(long periodStart, long periodEnd) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.values = new ArrayList<>();
    }
    
    @Override
    public void addValueTime(IValueTime<DataValue> vt) {
        this.values.add(vt);
    }

    @Override
    public void done() {
        if(done)
            throw new ShouldNeverHappenException("Should not call done() more than once.");
        done = true;
    }

    @Override
    public long getPeriodStartTime() {
        return periodStart;
    }

    @Override
    public long getPeriodEndTime() {
        return periodEnd;
    }
    
    public List<IValueTime<DataValue>> getValues(){
        return this.values;
    }

}
