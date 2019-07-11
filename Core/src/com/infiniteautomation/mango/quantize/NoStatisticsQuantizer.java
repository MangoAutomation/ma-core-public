/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
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

    private NoStatisticsGenerator stats;
    
    /**
     * @param bucketCalculator
     * @param callback
     */
    public NoStatisticsQuantizer(BucketCalculator bucketCalculator,
            StatisticsGeneratorQuantizerCallback<NoStatisticsGenerator> callback) {
        super(bucketCalculator, callback);
    }

    @Override
    protected NoStatisticsGenerator createStatistics(Instant start, Instant end,
            IValueTime startValue) {
        if(stats == null) {
            stats = new NoStatisticsGenerator(start.toEpochMilli(), end.toEpochMilli());
        }else {
            stats.reset(start.toEpochMilli(), end.toEpochMilli(), startValue);
        }
        return stats;
    }

}
