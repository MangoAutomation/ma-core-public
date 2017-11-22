package com.serotonin.m2m2.view.quantize3;

import java.time.Instant;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;
import com.serotonin.m2m2.view.stats.ValueChangeCounter;

public class ValueChangeCounterQuantizer extends AbstractPointValueTimeQuantizer<ValueChangeCounter> {

    public ValueChangeCounterQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<StatisticsGenerator> callback) {
        super(bucketCalculator, callback);
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.view.quantize3.AbstractDataQuantizer#createStatistics(java.time.Instant, java.time.Instant, com.serotonin.m2m2.rt.dataImage.types.DataValue)
     */
    @Override
    protected ValueChangeCounter createStatistics(Instant start, Instant end,
            DataValue startValue) {
        return new ValueChangeCounter(start.toEpochMilli(), end.toEpochMilli(), startValue);
    }
}
