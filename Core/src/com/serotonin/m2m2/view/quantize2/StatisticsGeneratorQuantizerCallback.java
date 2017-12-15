/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.quantize2;

import com.serotonin.m2m2.view.stats.StatisticsGenerator;

@Deprecated //Use com.infiniteautomation.mango.quantize class instead
public interface StatisticsGeneratorQuantizerCallback<T extends StatisticsGenerator> {
    /**
     * @param statisticsGenerator
     *            the statistics generated for the time period.
     */
    void quantizedStatistics(T statisticsGenerator);
}
