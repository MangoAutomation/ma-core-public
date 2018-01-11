package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

public class StartsAndRuntimeListQuantizer extends AbstractPointValueTimeQuantizer<StartsAndRuntimeList> {

    public StartsAndRuntimeListQuantizer(BucketCalculator bucketCalculator, StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList> callback) {
        super(bucketCalculator, callback);
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.view.quantize3.AbstractDataQuantizer#createStatistics(java.time.Instant, java.time.Instant, com.serotonin.m2m2.rt.dataImage.types.DataValue)
     */
    @Override
    protected StartsAndRuntimeList createStatistics(Instant start, Instant end,
            DataValue startValue) {
        return new StartsAndRuntimeList(start.toEpochMilli(), end.toEpochMilli(), startValue);
    }
}
