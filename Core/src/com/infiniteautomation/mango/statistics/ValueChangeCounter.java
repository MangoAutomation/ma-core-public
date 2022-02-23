/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.statistics;

import java.util.List;
import java.util.Objects;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.dao.pointvalue.ChangesAggregate;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * @author Matthew Lohbihler
 */
public class ValueChangeCounter implements StatisticsGenerator, ChangesAggregate {
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
    private long count;
    private int changes;

    // State values
    private DataValue latestValue;

    public ValueChangeCounter(long periodStart, long periodEnd, IValueTime<DataValue> startVT,
            List<? extends IValueTime<DataValue>> values) {
        this(periodStart, periodEnd, startVT);
        for (IValueTime<DataValue> p : values)
            addValueTime(p);
        done();
    }

    public ValueChangeCounter(long periodStart, long periodEnd, IValueTime<DataValue> startValue) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
      //Check for null and also bookend values
        if(startValue != null && startValue.getValue() != null) {
            latestValue = this.startValue = startValue.getValue();
        }
    }

    @Override
    public void addValueTime(IValueTime<DataValue> vt) {
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

    @Override
    public DataValue getStartValue() {
        return startValue;
    }

    @Override
    public DataValue getFirstValue() {
        return firstValue;
    }

    @Override
    public Long getFirstTime() {
        return firstTime;
    }

    @Override
    public DataValue getLastValue() {
        return lastValue;
    }

    @Override
    public Long getLastTime() {
        return lastTime;
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
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
