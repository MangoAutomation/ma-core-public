/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.view.stats.IValueTime;

public class StartsAndRuntimeListQuantizer extends AbstractPointValueTimeQuantizer<StartsAndRuntimeList> {

    private StartsAndRuntimeList stats;
    
    public StartsAndRuntimeListQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList> callback) {
        super(bucketCalculator, callback);
    }

    @Override
    protected StartsAndRuntimeList createStatistics(Instant start, Instant end,
            IValueTime startValue) {
        if(stats == null) {
            stats = new StartsAndRuntimeList(start.toEpochMilli(), end.toEpochMilli(), startValue);
        }else {
            stats.reset(start.toEpochMilli(), end.toEpochMilli(), startValue);
        }
        return stats;
    }
}
