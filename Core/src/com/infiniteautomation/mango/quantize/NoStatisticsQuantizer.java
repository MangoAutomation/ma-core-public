/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.Instant;

import com.infiniteautomation.mango.statistics.NoStatisticsGenerator;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;

/**
 *
 * @author Terry Packer
 */
public class NoStatisticsQuantizer extends AbstractPointValueTimeQuantizer<NoStatisticsGenerator>{

    public NoStatisticsQuantizer(BucketCalculator bucketCalculator) {
        super(bucketCalculator, null);
    }

    public NoStatisticsQuantizer(BucketCalculator bucketCalculator,
            StatisticsGeneratorQuantizerCallback<NoStatisticsGenerator> callback) {
        super(bucketCalculator, callback);
    }

    @Override
    protected NoStatisticsGenerator createStatistics(Instant start, Instant end,
            IValueTime<DataValue> startValue) {
        return new NoStatisticsGenerator(start.toEpochMilli(), end.toEpochMilli());
    }

}
