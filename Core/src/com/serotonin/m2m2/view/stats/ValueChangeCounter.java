/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.stats;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Matthew Lohbihler
 */
public class ValueChangeCounter implements StatisticsGenerator {
    // Configuration values.
    private final long periodStart;
    private final long periodEnd;

    // Calculated values
    private DataValue firstValue;
    private Long firstTime;
    private DataValue lastValue;
    private Long lastTime;
    private int count;
    private int changes;

    // State values
    private DataValue latestValue;

    public ValueChangeCounter(long periodStart, long periodEnd, PointValueTime startVT,
            List<? extends IValueTime> values) {
        this(periodStart, periodEnd, startVT == null ? null : startVT.getValue(), values);
    }

    public ValueChangeCounter(long periodStart, long periodEnd, DataValue startValue, List<? extends IValueTime> values) {
        this(periodStart, periodEnd, startValue);
        for (IValueTime p : values)
            addValueTime(p);
        done();
    }

    public ValueChangeCounter(long periodStart, long periodEnd, DataValue startValue) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        latestValue = startValue;
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

        if (!ObjectUtils.equals(latestValue, value)) {
            changes++;
            latestValue = value;
        }

        lastValue = value;
        lastTime = time;
    }

    @Override
    public void done(IValueTime endVT) {
        done();
    }

    public void done() {
        // no op
    }

    @Override
    public long getPeriodStartTime() {
        return periodStart;
    }

    @Override
    public long getPeriodEndTime() {
        return periodEnd;
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
        return "{count: " + count + ", changes: " + changes + ", firstValue: " + firstValue + ", firstTime: "
                + firstTime + ", lastValue: " + lastValue + ", lastTime: " + lastTime + ", periodStart: " + periodStart
                + ", periodEnd: " + periodEnd + "}";
    }
}
