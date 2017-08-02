package com.serotonin.m2m2.view.quantize2;

import java.util.List;

import org.joda.time.DateTime;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.ValueChangeCounter;

public class ValueChangeCounterQuantizer extends AbstractDataQuantizer {
    public static void quantize(BucketCalculator bucketCalculator, DataValue startValue, List<IValueTime> data,
            StatisticsGeneratorQuantizerCallback<ValueChangeCounter> callback) {
        ValueChangeCounterQuantizer qt = new ValueChangeCounterQuantizer(bucketCalculator, startValue, callback);
        qt.data(data);
    }

    private final StatisticsGeneratorQuantizerCallback<ValueChangeCounter> callback;
    private ValueChangeCounter valueChangeCounter;

    public ValueChangeCounterQuantizer(BucketCalculator bucketCalculator, DataValue startValue,
            StatisticsGeneratorQuantizerCallback<ValueChangeCounter> callback) {
        super(bucketCalculator, startValue);
        this.callback = callback;
    }

    @Override
    protected void openPeriod(DateTime start, DateTime end, DataValue startValue) {
        valueChangeCounter = new ValueChangeCounter(start.getMillis(), end.getMillis(), startValue);
    }

    @Override
    protected void dataInPeriod(DataValue value, long time) {
        valueChangeCounter.addValue(value, time);
    }

    @Override
    protected void closePeriod() {
        if (valueChangeCounter != null) {
            valueChangeCounter.done();
            callback.quantizedStatistics(valueChangeCounter);
        }
    }
}
