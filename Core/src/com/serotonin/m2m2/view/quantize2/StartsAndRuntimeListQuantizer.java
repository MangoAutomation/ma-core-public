package com.serotonin.m2m2.view.quantize2;

import java.util.List;

import org.joda.time.DateTime;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StartsAndRuntimeList;

public class StartsAndRuntimeListQuantizer extends AbstractDataQuantizer {
    public static void quantize(BucketCalculator bucketCalculator, DataValue startValue, List<IValueTime> data,
    	StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList> callback) {
        StartsAndRuntimeListQuantizer qt = new StartsAndRuntimeListQuantizer(bucketCalculator, startValue, callback);
        qt.data(data);
    }

    private final StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList> callback;
    private StartsAndRuntimeList startsAndRuntimeList;

    public StartsAndRuntimeListQuantizer(BucketCalculator bucketCalculator, DataValue startValue,
            StatisticsGeneratorQuantizerCallback<StartsAndRuntimeList> callback) {
        super(bucketCalculator, startValue);
        this.callback = callback;
    }

    @Override
    protected void openPeriod(DateTime start, DateTime end, DataValue startValue) {
        startsAndRuntimeList = new StartsAndRuntimeList(start.getMillis(), end.getMillis(), startValue);
    }

    @Override
    protected void dataInPeriod(DataValue value, long time) {
        startsAndRuntimeList.addValueTime(value, time);
    }

    @Override
    protected void closePeriod() {
        if (startsAndRuntimeList != null) {
            startsAndRuntimeList.done();
            callback.quantizedStatistics(startsAndRuntimeList);
        }
    }
}
