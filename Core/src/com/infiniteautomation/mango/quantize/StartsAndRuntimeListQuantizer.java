package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.view.stats.IValueTime;

public class StartsAndRuntimeListQuantizer extends AbstractPointValueTimeQuantizer<StartsAndRuntimeList> {

    public StartsAndRuntimeListQuantizer(BucketCalculator bucketCalculator) {
        super(bucketCalculator, null);
    }

    public StartsAndRuntimeListQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList> callback) {
        super(bucketCalculator, callback);
    }

    @Override
    protected StartsAndRuntimeList createStatistics(Instant start, Instant end,
            IValueTime startValue) {
        return new StartsAndRuntimeList(start.toEpochMilli(), end.toEpochMilli(), startValue);
    }
}
