/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
