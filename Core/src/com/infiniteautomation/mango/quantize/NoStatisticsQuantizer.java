/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.NoStatisticsGenerator;
import com.serotonin.m2m2.view.stats.IValueTime;

/**
 *
 * @author Terry Packer
 */
public class NoStatisticsQuantizer extends AbstractPointValueTimeQuantizer<NoStatisticsGenerator>{

    /**
     */
    public NoStatisticsQuantizer(BucketCalculator bucketCalculator,
            StatisticsGeneratorQuantizerCallback<NoStatisticsGenerator> callback) {
        super(bucketCalculator, callback);
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.quantize.AbstractPointValueTimeQuantizer#createStatistics(java.time.Instant, java.time.Instant, com.serotonin.m2m2.rt.dataImage.types.DataValue)
     */
    @Override
    protected NoStatisticsGenerator createStatistics(Instant start, Instant end,
            IValueTime startValue) {
        return new NoStatisticsGenerator(start.toEpochMilli(), end.toEpochMilli());
    }

}
