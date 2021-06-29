/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.AnalogStatistics;
import com.serotonin.m2m2.view.stats.IValueTime;

public class AnalogStatisticsQuantizer extends AbstractPointValueTimeQuantizer<AnalogStatistics> {

    public AnalogStatisticsQuantizer(BucketCalculator bucketCalculator,
            StatisticsGeneratorQuantizerCallback<AnalogStatistics> callback) {
        super(bucketCalculator, callback);
    }

    @Override
    protected AnalogStatistics createStatistics(Instant start, Instant end, IValueTime startValue) {
        return new AnalogStatistics(start.toEpochMilli(), end.toEpochMilli(), startValue);
    }

}
