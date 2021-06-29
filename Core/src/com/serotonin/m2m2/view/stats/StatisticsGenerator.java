/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.stats;

/**
 * Statistics generators calculate rollup values for a time period. Values for the period are added using the
 * addValueTime method. When there is no more data, the done method is called. Things to consider:
 * 
 * <ul>
 * <li>A period may have no data.</li>
 * <li>A period may or may not have a starting value (i.e. the value that was current at the moment the period started.)
 * </li>
 * <li>A period may or may not have an ending value (i.e. the first value that occurs after the period ends.)</li>
 * </ul>
 * 
 * @author Matthew Lohbihler
 */
public interface StatisticsGenerator {
    /**
     * Used to add values to a period
     * 
     * @param vt
     *            the value to add
     */
    void addValueTime(IValueTime vt);

    /**
     * Used to end a period
     * 
     */
    void done();

    /**
     * @return the start time of the period
     */
    long getPeriodStartTime();

    /**
     * @return the end time of the period
     */
    long getPeriodEndTime();
}
