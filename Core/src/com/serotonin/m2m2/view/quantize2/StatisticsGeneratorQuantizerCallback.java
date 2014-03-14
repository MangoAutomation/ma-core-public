/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.quantize2;

import com.serotonin.m2m2.view.stats.StatisticsGenerator;

public interface StatisticsGeneratorQuantizerCallback<T extends StatisticsGenerator> {
    /**
     * @param statisticsGenerator
     *            the statistics generated for the time period.
     * @param done
     *            whether there is any more data in the data set. This will be true even if the last of the data
     *            occurred in the current time period. I.e. you should check if done is true and, e.g. if stats.count >
     *            0 if you want to exclude periods.
     */
    void quantizedStatistics(T statisticsGenerator, boolean done);
}
