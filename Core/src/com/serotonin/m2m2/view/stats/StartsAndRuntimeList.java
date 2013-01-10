/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Matthew Lohbihler
 */
public class StartsAndRuntimeList implements StatisticsGenerator {
    // Configuration values.
    private final long periodStart;
    private final long periodEnd;

    // Calculated values.
    private DataValue firstValue;
    private Long firstTime;
    private DataValue lastValue;
    private Long lastTime;
    private final List<StartsAndRuntime> data = new ArrayList<StartsAndRuntime>();

    // State values.
    private DataValue latestValue;
    private long latestTime;
    private StartsAndRuntime sar;

    public StartsAndRuntimeList(long periodStart, long periodEnd, PointValueTime startVT,
            List<? extends IValueTime> values, PointValueTime endVT) {
        this(periodStart, periodEnd, startVT == null ? null : startVT.getValue(), values, endVT == null ? null : endVT
                .getValue());
    }

    public StartsAndRuntimeList(long periodStart, long periodEnd, DataValue startValue,
            List<? extends IValueTime> values, DataValue endValue) {
        this(periodStart, periodEnd, startValue);
        for (IValueTime vt : values)
            addValueTime(vt);
        done(endValue);
    }

    public StartsAndRuntimeList(long periodStart, long periodEnd, DataValue startValue) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;

        if (startValue != null) {
            latestValue = startValue;
            latestTime = periodStart;
            sar = get(startValue);
        }
    }

    @Override
    public void addValueTime(IValueTime vt) {
        addValueTime(vt.getValue(), vt.getTime());
    }

    public void addValueTime(DataValue value, long time) {
        if (value == null)
            return;

        if (firstValue == null) {
            firstValue = value;
            firstTime = time;
        }

        if (!ObjectUtils.equals(value, latestValue)) {
            // Update the last value stats, if any.
            if (sar != null)
                sar.runtime += time - latestTime;

            latestValue = value;
            latestTime = time;
            sar = get(value);
            sar.starts++;
        }

        lastValue = value;
        lastTime = time;
    }

    @Override
    public void done(IValueTime endVT) {
        done(endVT == null ? null : endVT.getValue());
    }

    public void done(DataValue endValue) {
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
        Collections.sort(data, new Comparator<StartsAndRuntime>() {
            @Override
            public int compare(StartsAndRuntime o1, StartsAndRuntime o2) {
                return o1.value.compareTo(o2.value);
            }
        });
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

    public Map<Object, StartsAndRuntime> getStartsAndRuntime() {
        Map<Object, StartsAndRuntime> result = new HashMap<Object, StartsAndRuntime>();
        for (StartsAndRuntime sar : data)
            result.put(sar.getValue(), sar);
        return result;
    }

    public List<StartsAndRuntime> getData() {
        return data;
    }

    public StartsAndRuntime get(Object value) {
        return get(DataValue.objectToValue(value));
    }

    public StartsAndRuntime get(DataValue value) {
        for (StartsAndRuntime sar : data) {
            if (ObjectUtils.equals(sar.value, value))
                return sar;
        }

        StartsAndRuntime sar = new StartsAndRuntime();
        sar.value = value;
        data.add(sar);

        return sar;
    }

    public String getHelp() {
        return toString();
    }

    @Override
    public String toString() {
        return "{data: " + data.toString() + ", periodStart: " + periodStart + ", periodEnd: " + periodEnd + "}";
    }
}
