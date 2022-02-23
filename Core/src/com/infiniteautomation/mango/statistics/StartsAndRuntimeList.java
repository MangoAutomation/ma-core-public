/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.dao.pointvalue.StartsAndRuntimeAggregate;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * Track runtime, state changes and percentage in state of total runtime (not period)
 * 
 * @author Matthew Lohbihler, Terry Packer
 */
public class StartsAndRuntimeList implements StatisticsGenerator, StartsAndRuntimeAggregate {
    // Configuration values.
    private final long periodStart;
    private final long periodEnd;
    private boolean done = false;

    // Calculated values.
    private DataValue startValue;
    private DataValue firstValue;
    private Long firstTime;
    private DataValue lastValue;
    private Long lastTime;
    private final List<StartsAndRuntime> data = new ArrayList<>();
    private long count;

    // State values.
    private long latestTime;
    private StartsAndRuntime sar;

    public StartsAndRuntimeList(long periodStart, long periodEnd, IValueTime<DataValue> startVT,
            List<? extends IValueTime<DataValue>> values) {
      this(periodStart, periodEnd, startVT);
      for (IValueTime<DataValue> vt : values)
          addValueTime(vt);
      done();
    }

    public StartsAndRuntimeList(long periodStart, long periodEnd, IValueTime<DataValue> startValue) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;

        //Check for null and also bookend values
        if (startValue != null && startValue.getValue() != null) {
            this.startValue = startValue.getValue();
            latestTime = periodStart;
            sar = get(this.startValue);
        }
    }

    @Override
    public void addValueTime(IValueTime<DataValue> vt) {
        addValueTime(vt.getValue(), vt.getTime());
    }

    public void addValueTime(DataValue value, long time) {
        if (value == null)
            return;

        count++;

        if (firstValue == null) {
            firstValue = value;
            firstTime = time;
        }

        if (sar != null)
            sar.runtime += time - latestTime;

        latestTime = time;
        sar = get(value);
        sar.starts++;
        lastValue = value;
        lastTime = time;
    }

    @Override
    public void done() {
        if(done)
            throw new ShouldNeverHappenException("Should not call done() more than once.");
        done = true;
        
        // If there is a current SAR, update 
        // if (endValue != null && sar != null)
        if (sar != null)
            sar.runtime += periodEnd - latestTime;

        // Calculate the total duration as the sum of the runtimes.
        long totalRuntime = 0;
        for (StartsAndRuntime s : data)
            totalRuntime += s.runtime;

        // Calculate runtime percentages.
        for (StartsAndRuntime s : data)
            s.calculateRuntimePercentage(totalRuntime);

        // Sort by value.
        data.sort((o1, o2) -> o1.value.compareTo(o2.value));
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
    public long getCount(){
        return count;
    }

    @Override
    public List<StartsAndRuntime> getData() {
        return Collections.unmodifiableList(data);
    }

    private StartsAndRuntime get(DataValue value) {
        for (StartsAndRuntime sar : data) {
            if (Objects.equals(sar.value, value))
                return sar;
        }

        StartsAndRuntime sar = new StartsAndRuntime(value);
        data.add(sar);

        return sar;
    }

    public String getHelp() {
        return toString();
    }

    @Override
    public String toString() {
        return "{data: " + data.toString() +
        		", periodStartTime: " + periodStart + 
        		", periodEndTime: " + periodEnd + 
        		", count: " + count +
        		", startValue: " + startValue +
        		", firstValue: " + firstValue +
        		", firstTime: " + firstTime + 
        		", lastValue: " + lastValue +
        		", lastTime: " + lastTime +
        		"}";
    }
}
