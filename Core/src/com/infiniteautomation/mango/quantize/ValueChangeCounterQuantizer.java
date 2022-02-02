package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.ValueChangeCounter;
import com.serotonin.m2m2.view.stats.IValueTime;

public class ValueChangeCounterQuantizer extends AbstractPointValueTimeQuantizer<ValueChangeCounter> {

    public ValueChangeCounterQuantizer(BucketCalculator bucketCalculator) {
        super(bucketCalculator, null);
    }

    public ValueChangeCounterQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<ValueChangeCounter> callback) {
        super(bucketCalculator, callback);
    }

    @Override
    protected ValueChangeCounter createStatistics(Instant start, Instant end,
            IValueTime startValue) {
        return new ValueChangeCounter(start.toEpochMilli(), end.toEpochMilli(), startValue);
    }
}
