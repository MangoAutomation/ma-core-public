/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import com.infiniteautomation.mango.statistics.AnalogStatistics;

/**
 * Wrapper to supply getHelp method and make the statistics read-only from the script
 * 
 * @author Terry Packer
 *
 */
public class AnalogStatisticsWrapper {

    private final AnalogStatistics statistics;

    public AnalogStatisticsWrapper(AnalogStatistics statistics) {
        this.statistics = statistics;
    }


    public long getPeriodStartTime() {
        return statistics.getPeriodStartTime();
    }

    public long getPeriodEndTime() {
        return statistics.getPeriodEndTime();
    }

    public Double getMinimumValue() {
        return statistics.getMinimumValue();
    }

    public Long getMinimumTime() {
        return statistics.getMinimumTime();
    }

    public Double getMaximumValue() {
        return statistics.getMaximumValue();
    }

    public Long getMaximumTime() {
        return statistics.getMaximumTime();
    }

    public Double getAverage() {
        return statistics.getAverage();
    }

    public Double getIntegral() {
        return statistics.getIntegral();
    }

    public double getSum() {
        return statistics.getSum();
    }

    public Double getStartValue() {
        return statistics.getStartValue() == null ? null : statistics.getStartValue().getDoubleValue();
    }

    public Double getFirstValue() {
        return statistics.getFirstValue() == null ? null : statistics.getFirstValue().getDoubleValue();
    }

    public Long getFirstTime() {
        return statistics.getFirstTime();
    }

    public Double getLastValue() {
        return statistics.getLastValue() == null ? null : statistics.getLastValue().getDoubleValue();
    }

    public Long getLastTime() {
        return statistics.getLastTime();
    }

    public int getCount() {
        return (int) statistics.getCount();
    }

    public double getDelta() {
        return statistics.getDelta();
    }

    public String getHelp() {
        return statistics.toString();
    }

    @Override
    public String toString() {
        return statistics.toString();
    }
}
