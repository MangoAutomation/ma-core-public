/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.quantize2;

import java.util.List;

import org.joda.time.DateTime;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.AnalogStatistics;
import com.serotonin.m2m2.view.stats.IValueTime;

@Deprecated //Use com.infiniteautomation.mango.quantize class instead
public class AnalogStatisticsQuantizer extends AbstractDataQuantizer {
    public static void quantize(BucketCalculator bucketCalculator, DataValue startValue, List<IValueTime> data,
    		StatisticsGeneratorQuantizerCallback<AnalogStatistics> callback) {
        AnalogStatisticsQuantizer qt = new AnalogStatisticsQuantizer(bucketCalculator, startValue, callback);
        qt.data(data);
    }

    private final StatisticsGeneratorQuantizerCallback<AnalogStatistics> callback;
    private AnalogStatistics analogStatistics;

    public AnalogStatisticsQuantizer(BucketCalculator bucketCalculator, DataValue startValue,
            StatisticsGeneratorQuantizerCallback<AnalogStatistics> callback) {
        super(bucketCalculator, startValue);
        this.callback = callback;
    }

    @Override
    protected void openPeriod(DateTime start, DateTime end, DataValue startValue) {
        analogStatistics = new AnalogStatistics(start.getMillis(), end.getMillis(), startValue == null ? null
                : startValue.getDoubleValue());
    }

    @Override
    protected void dataInPeriod(DataValue value, long time) {
        analogStatistics.addValueTime(value, time);
    }

    @Override
    protected void closePeriod() {
        if (analogStatistics != null) {
            analogStatistics.done();
            callback.quantizedStatistics(analogStatistics);
        }
    }
}
