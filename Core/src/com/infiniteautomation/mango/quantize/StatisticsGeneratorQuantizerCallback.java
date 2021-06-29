/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import com.infiniteautomation.mango.db.query.QueryCancelledException;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

public interface StatisticsGeneratorQuantizerCallback<T extends StatisticsGenerator> {
    /**
     * @param statisticsGenerator
     *            the statistics generated for the time period.
     */
    void quantizedStatistics(T statisticsGenerator) throws QueryCancelledException;
}
