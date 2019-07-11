/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.ValueChangeCounter;
import com.serotonin.m2m2.view.stats.IValueTime;

public class ValueChangeCounterQuantizer extends AbstractPointValueTimeQuantizer<ValueChangeCounter> {

    private ValueChangeCounter stats;
    
    public ValueChangeCounterQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<ValueChangeCounter> callback) {
        super(bucketCalculator, callback);
    }
    
    @Override
    protected ValueChangeCounter createStatistics(Instant start, Instant end,
            IValueTime startValue) {
        if(stats == null) {
            stats = new ValueChangeCounter(start.toEpochMilli(), end.toEpochMilli(), startValue);
        }else {
            stats.reset(start.toEpochMilli(), end.toEpochMilli(), startValue);
        }
        return stats;
    }
}
