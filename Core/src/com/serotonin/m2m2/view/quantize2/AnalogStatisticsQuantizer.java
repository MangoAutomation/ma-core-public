/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.quantize2;

import java.util.List;

import org.joda.time.DateTime;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.AnalogStatistics;
import com.serotonin.m2m2.view.stats.IValueTime;

public class AnalogStatisticsQuantizer extends AbstractDataQuantizer {
    public static void quantize(BucketCalculator bucketCalculator, DataValue startValue, List<IValueTime> data,
            DataValue endValue, StatisticsGeneratorQuantizerCallback<AnalogStatistics> callback) {
        AnalogStatisticsQuantizer qt = new AnalogStatisticsQuantizer(bucketCalculator, startValue, callback);
        qt.data(data, endValue);
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
    protected void closePeriod(DataValue endValue) {
        if (analogStatistics != null) {
            analogStatistics.done(endValue == null ? null : endValue.getDoubleValue());
            callback.quantizedStatistics(analogStatistics, endValue == null);
        }
    }
}
