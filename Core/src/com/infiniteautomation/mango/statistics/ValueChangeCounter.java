/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.statistics;

import java.util.List;
import java.util.Objects;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * @author Matthew Lohbihler
 */
public class ValueChangeCounter implements StatisticsGenerator {
    // Configuration values.
    private final long periodStart;
    private final long periodEnd;
    private boolean done = false;

    // Calculated values
    private DataValue startValue;
    private DataValue firstValue;
    private Long firstTime;
    private DataValue lastValue;
    private Long lastTime;
    private int count;
    private int changes;

    // State values
    private DataValue latestValue;

    public ValueChangeCounter(long periodStart, long periodEnd, IValueTime startVT,
            List<? extends IValueTime> values) {
        this(periodStart, periodEnd, startVT);
        for (IValueTime p : values)
            addValueTime(p);
        done();
    }

    public ValueChangeCounter(long periodStart, long periodEnd, IValueTime startValue) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
      //Check for null and also bookend values
        if(startValue != null && startValue.getValue() != null) {
            latestValue = this.startValue = startValue.getValue();
        }
    }

    @Override
    public void addValueTime(IValueTime vt) {
        addValue(vt.getValue(), vt.getTime());
    }

    public void addValue(DataValue value, long time) {
        if (value == null)
            return;

        count++;

        if (firstValue == null) {
            firstValue = value;
            firstTime = time;
        }

        if (!Objects.equals(latestValue, value)) {
            changes++;
            latestValue = value;
        }

        lastValue = value;
        lastTime = time;
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
    
    public DataValue getStartValue() {
        return startValue;
    }
    
    public DataValue getFirstValue() {
        return firstValue;
    }

    public Long getFirstTime() {
        return firstTime;
    }

    public DataValue getLastValue() {
        return lastValue;
    }

    public Long getLastTime() {
        return lastTime;
    }

    public int getCount() {
        return count;
    }

    public int getChanges() {
        return changes;
    }

    public String getHelp() {
        return toString();
    }

    @Override
    public String toString() {
        return "{count: " + count + 
                ", changes: " + changes + 
                ", startValue: " + startValue +
                ", firstValue: " + firstValue + 
                ", firstTime: " + firstTime + 
                ", lastValue: " + lastValue + 
                ", lastTime: " + lastTime + 
                ", periodStartTime: " + periodStart
                + ", periodEndTime: " + periodEnd + "}";
    }
}
